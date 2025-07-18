package tm.preprocessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import tm.ui.Loggable;
import tm.utils.IOUtils;
import au.com.bytecode.opencsv.CSVReader;

/**Pre-processes documents in a streaming fashion (i.e., a doc is loaded and then ran through the pipeline, versus
 * all docs being loaded and ran through the pipeline as a batch. The immediate advantage is a much lower memory footprint.
 * The only big object that is kept in memory throughout is the feature set.
 * 
 * @author bsimpkins
 *
 */
public class StreamingPreprocessor 
{
	private Properties props;
	
	/**When pulling docs from the source db, this is the size of batching */
	private final static int DB_BATCH_SELECT_SIZE=100;
	
	private Map<String,Feature> featureMap = new HashMap<String,Feature>();
	
	private SimpleTokenizer tokenizer;
	private FeatureSelector featureSelector;
	private ArrayList<String> tokenList = new ArrayList<String>();
	private Map<String,Feature> tempFeatMap = new HashMap<String,Feature>();
	private List<Feature> removedFeatures = new ArrayList<Feature>();
	
	private ArrayList<Document> docList = new ArrayList<Document>();
	
	private StringBuffer docBuff = new StringBuffer();
	private StringBuffer docObjBuff = new StringBuffer();
	
	private long featureId = 0;
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static String MODEL_DIR;
	
	private ArrayList<Loggable> loggableList = new ArrayList<Loggable>();
	
	private boolean interrupt = false;
	
	public StreamingPreprocessor(String propsFilename)
	{
		props = IOUtils.loadPropsFile(propsFilename);
		
		//Create model directory if it's not there
		MODEL_DIR = props.getProperty("TopicModeler.TopicModel.Model.Dir");
		if (!MODEL_DIR.endsWith("/")) MODEL_DIR += "/";
		File dir = new File(MODEL_DIR);
		if (!dir.exists()) {
			if (!dir.mkdir())
				addLogError("StreamingProcessor(): Could not make project directory!!");
		}
		
		tokenizer = new SimpleTokenizer(props);
		featureSelector = new FeatureSelector(props,this);
		
		
	}
	
	public StreamingPreprocessor(Properties props)
	{
		this.props = props;
		//Create model directory if it's not there
		MODEL_DIR = props.getProperty("TopicModeler.TopicModel.Model.Dir");
		if (!MODEL_DIR.endsWith("/")) MODEL_DIR += "/";
		File dir = new File(MODEL_DIR);
		if (!dir.exists()) {
			if (!dir.mkdir())
				addLogError("StreamingProcessor(): Could not make project directory!!");
		}
		
		tokenizer = new SimpleTokenizer(props);
		featureSelector = new FeatureSelector(props,this);
	}
	
	public boolean processDocs()
	{
		String docSource = props.getProperty("TopicModeler.Preprocessor.DocumentSource").toLowerCase();
		if (docSource.equals("db"))
		{
			addLogOut("StreamingPreprocessor(): Processing docs from files...");
			processDocsFromDB();
		}
		else if (docSource.contains("file"))
		{
			addLogOut("StreamingPreprocessor(): Processing docs from database...");
			processDocsFromFiles(); 
		}else
		{
			addLogError("StreamingPreprocessor(): Document source not recognized!!");
		}
		if (interrupt)
		{
			interrupt = false;
			return false;
		}
		featureSelector.setFeatures(featureMap);
		featureSelector.removeMaxAndMinFeatDocsCounts(docList);
		
		if (interrupt)
		{
			interrupt = false;
			return false;
		}
		
		writeOutFeatures();
		writeOutDocuments();
		
		return true;
	}
	
	private void processDocsFromDB()
	{
		Connection conn = IOUtils.initSimpleConnectionForSourceDB(props);
		
		String idQuery = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocId.Query");
		String docQueryStem = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.Query");
		String docIdField = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.IdField");
		
		String whereFieldsAll = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.WhereValues");
		String[] whereFields = whereFieldsAll.split(",");
		
		String adnlClauses = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.AdditionalClauses");
		
		String docQuery = docQueryStem + " WHERE ";
		for (int i = 0; i < whereFields.length; i++)
		{
			docQuery += whereFields[i]+ " AND ";
		}
		StringBuffer inBuff = new StringBuffer();
		ResultSet rs = null;
		try {
			rs = conn.prepareStatement(idQuery).executeQuery();
			int count = 0;
			
			while(rs.next() && !interrupt)
			{
				int id = rs.getInt(1);
				inBuff.append(id);
				if (++count % DB_BATCH_SELECT_SIZE == 0)
				{
					//Construct IN clause of doc_content query
					docQuery += docIdField+" IN ("+inBuff.toString()+") ";
					
					//Add additional clauses..
					docQuery += " "+adnlClauses;
					
					addLogOut("StreamingProcessor.processDocsFromDB(): 100 docs: "+count);
					
					//addLogOut("doc_content query: "+docQuery);
					ResultSet rs2 = conn.prepareStatement(docQuery).executeQuery();
					
					//Process docs
					while(rs2.next() && !interrupt)
					{
						processSingleDoc(rs2.getString(3), rs2.getTimestamp(2),rs2.getLong(1));
					}
					
					inBuff.setLength(0);
					docQuery = docQueryStem + " WHERE ";
					for (int i = 0; i < whereFields.length; i++)
					{
						docQuery += whereFields[i]+ " AND ";
					}
					
					
				}
				else inBuff.append(",");
				
				
			}
		} catch (SQLException e1) {
			addLogError("StreamingProcessor.processDocsForDB(): Problem with loading docs from db!!");
			e1.printStackTrace();
		}
		
		if (interrupt)
		{
			return;
		}
		
		if (inBuff.length() != 0)
		{
			//There's still some left over (if number of ids doesn't divide evently into DB_BATCH_SELECT_SIZE). Process them
			//Construct IN clause of doc_content query
			docQuery += docIdField+" IN ("+inBuff.toString();
			docQuery = docQuery.substring(0,docQuery.length()-1)+")";
		
			//Add additional clauses..
			docQuery += " "+adnlClauses;
			
			//addLogOut("doc_content query: "+docQuery);
			ResultSet rs2;
			try {
				rs2 = conn.prepareStatement(docQuery).executeQuery();
				while(rs2.next())
				{
					processSingleDoc(rs2.getString(3), rs2.getTimestamp(2),rs2.getInt(1));
				}
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private void processDocsFromFiles()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");
		
		String docDir = props.getProperty("TopicModeler.Preprocessor.SourceFile.Directory");
		if (!docDir.endsWith("/")) docDir += "/";
		
		String docExtension = props.getProperty("TopicModeler.Preprocessor.SourceFile.Extension");
		if (!docExtension.startsWith(".")) docExtension = "."+docExtension;
		
		String metadataDir = props.getProperty("TopicModeler.Preprocessor.SourceFile.Metadata.Directory");
		if (!metadataDir.endsWith("/")) metadataDir += "/";
		
		String metadataInfoFile = props.getProperty("TopicModeler.Preprocessor.SourceFile.Metadata.InfoFile");
		
		Map<String,String[]> mdMap = IOUtils.loadMetadataInfo(docDir+metadataDir+metadataInfoFile);
		String[] docTimeValFile = mdMap.get("time");
		if (docTimeValFile == null)
		{
			addLogError("StreamingProcessor.processDocsFromFiles(): Cannot locate time metadata in metadata info: "+docDir+metadataDir+metadataInfoFile);
//			addLogError("Exiting!!!");			
//			System.exit(1);
		}
		
		if (!docTimeValFile[1].equals("timestamp"))
		{
			addLogError("StreamingProcessor.processDocsFromFiles(): Time metadata must be a timestamp value (yyyy-MM-dd HH:mm:ss)");
//			addLogError("Exiting!!!");			
//			System.exit(1);
		}
		
		try {
			addLogOut("StreamingProcessor.processDocsFromFiles(): Loading doc time file: "+docDir+metadataDir+docTimeValFile[0]);
			CSVReader reader = new CSVReader(new FileReader(new File(docDir+metadataDir+docTimeValFile[0])));
			
			String[] line;
			Date[] dateRange = new Date[2];
			String[] dateRangeSpl = props.getProperty("TopicModeler.Preprocessor.SourceFile.Timerange").split(",");
			try {
				dateRange[0] = sdf.parse(dateRangeSpl[0].trim());
				dateRange[1] = sdf.parse(dateRangeSpl[1].trim());
			} catch (ParseException e1) {
				//try to parse with date format
				try {
					dateRange[0] = sdfDay.parse(dateRangeSpl[0].trim());
					dateRange[1] = sdfDay.parse(dateRangeSpl[1].trim());
				} catch (ParseException e) {
					//Should have been caught in pp validation..
					addLogError("StreamingProcessor.processDocsFromFiles(): Property TopicModeler.Preprocessor.SourceFile.Timerange not in correct format (yyyy-MM-dd HH:mm:ss) or (yyyy-MM-dd)");
					e.printStackTrace();
				}

			}
			
			int count = 0;
			while((line=reader.readNext()) != null && !interrupt)
			{
				
				
				int docId = Integer.parseInt(line[0]);
				Timestamp time = null;
				try {
					time = new Timestamp(sdf.parse(line[1]).getTime());
					
				} catch (ParseException e) {
					try {
						time = new Timestamp(sdfDay.parse(line[1]).getTime());
					} catch (ParseException e1) {
						addLogError("StreamingProcessor.processDocsFromFiles(): Time metadata not in correct format (yyyy-MM-dd HH:mm:ss)");
						addLogError("Exiting!!!");					
						System.exit(1);
					}
					

				}
				
				//Only process if within time range
				if (time.getTime() >= dateRange[0].getTime() && time.getTime() <= dateRange[1].getTime())
				{
					if (++count % DB_BATCH_SELECT_SIZE == 0)
					{
						addLogOut("StreamingProcessor.processDocsFiles(): 100 docs: "+count);
					}
					
					String content = IOUtils.loadFileAsString(new File(docDir+docId+docExtension));
					processSingleDoc(content, time, docId);
				}
				
			}
			reader.close();
			
			//Copy info.csv and all metadata files to model dir
//			Iterator<Entry<String,String[]>> mdIter = mdMap.entrySet().iterator();
//			while(mdIter.hasNext())
//			{
//				Entry<String,String[]> e = mdIter.next();
//				String[] mdInfoVal = e.getValue();
//				File mdFile = new File(docDir+metadataDir+e.getValue()[0]);
//				if (!mdFile.exists())
//				{
//					addLogError("StreamingPreprocessor.processDocsFromFiles(): Metadata file missing: "+e.getValue()[0]);
//				}else
//				{
//					File newFile = new File(MODEL_DIR+e.getValue()[0]);
//					IOUtils.copyFile(mdFile, newFile);
//				}
//				
//			}
			//Easier way: Just copy full metadata dir to MODELDIR
			
			//Make a hashmap for docs to speed things up in long run
			Set<Long> docSet= new HashSet<Long>();
			for(Document doc:docList)
			{
				docSet.add(doc.getExternalId());
			}
			
			File newMDDir = new File(MODEL_DIR+metadataDir);
			if (!newMDDir.mkdir())
			{
				//TODO: Handle with error at least.
			}
			
			//Copy over info file
			IOUtils.copyFile(new File(docDir+metadataDir+"info.csv"), new File( MODEL_DIR+metadataDir+"info.csv"));
			
			//Copy over metadata files for documents that are being used
			Iterator<Entry<String,String[]>> iter = mdMap.entrySet().iterator();
			while(iter.hasNext())
			{
				Entry<String,String[]> e = iter.next();
				
				FileWriter fw = new FileWriter(new File(MODEL_DIR+metadataDir+e.getValue()[0]));
				BufferedWriter bw = new BufferedWriter(fw);				
				StringBuffer sBuff = new StringBuffer();
				List<String[]> rows = IOUtils.loadCSV(new File(docDir+metadataDir+e.getValue()[0]));
				for(String[] row:rows)
				{
					sBuff.setLength(0);
					sBuff.append(row[0]).append(",\"").append(row[1]).append("\"\n");
					if (docSet.contains(Long.parseLong(row[0]))) bw.write(sBuff.toString());
				}
				bw.close();
			}
			
			//Copies all of metadata files. Not efficient because model can use a subset of docs/metadata.
			//IOUtils.copyFolder(new File(docDir+metadataDir), new File(MODEL_DIR+metadataDir));
			
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e)
		{
			
		}
	}
	
	/**Runs a single doc through the pipeline
	 * 
	 * @param doc
	 */
	private void processSingleDoc(String docStr,Timestamp time,long id)
	{
		//Run pipeline
		tokenizer.tokenize(docStr, tokenList);
		Document doc = new Document();
		doc.setExternalId(id);
		doc.setTime(time);
		tokensToFeaturesAndDoc(tokenList, tempFeatMap,doc);
		
		featureSelector.setFeatures(tempFeatMap);
		featureSelector.removeStopWords(removedFeatures);
		doc.removeFeatures(removedFeatures);
		
		if (Boolean.parseBoolean(props.getProperty("TopicModeler.Preprocessor.FeatureSelector.RemoveNumbers")))
				featureSelector.removeNumbers(removedFeatures);
		doc.removeFeatures(removedFeatures);
		
		mergeToFeatureMap(tempFeatMap);
		
		docList.add(doc);
		//Add to all features
		//featureMap.putAll(tempFeatMap);
	
		//Update feature counts
//		Iterator<String> iter = featureCountMap.keySet().iterator();
//		while(iter.hasNext())
//		{
//			String feat = iter.next();
//			if (!tempFeatMap.containsKey(feat)) iter.remove();
//		}
		////Write to doc file
//		writeOutDocument(tempFeatMap,featCountMap);
		
	}
	
	public void stopProcessing()
	{
		interrupt = true;
		featureSelector.interrupt();
		
	}
	
	private void writeOutFeatures()
	{
		//Sort features
		ArrayList<Feature> sortedList = sortFeatureMap(featureMap);
		addLogOut("StreamingProcessor.writeOutFeatures(): Number of features="+sortedList.size());
		
		try {
			FileWriter fw = new FileWriter(new File(MODEL_DIR+"features.txt"));
			BufferedWriter bw = new BufferedWriter(fw);

			for (Feature f:sortedList)
			{
//				bw.write(f.getId()+","+f.getName()+"\n");
				bw.write(f.getName()+"\n");
			}
			
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void writeOutDocuments()
	{
		BufferedWriter docWriter = null;
		BufferedWriter docObjWriter = null;
		//init doc file out stream
		try {
			FileWriter fw = new FileWriter(new File(MODEL_DIR+"docs.txt"));
			docWriter = new BufferedWriter(fw);
			
			FileWriter fwObj = new FileWriter(new File(MODEL_DIR+"docObjs.csv"));
			docObjWriter = new BufferedWriter(fwObj);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int docId = 1;
		for (Document d:docList)
		{
			d.setId(docId++);
			writeOutDocument(d,docWriter,docObjWriter);
		}
		
		try {
			docWriter.close();
			docObjWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	private void writeOutDocument(Map<String,Feature> featMap, Map<String,Integer> featCountMap)
//	{
//		sBuff.setLength(0);
//		sBuff.append(featMap.size()).append(" ");
//		Iterator<Entry<String,Feature>> iter = featMap.entrySet().iterator();
//		while(iter.hasNext())
//		{
//			Entry<String,Feature> e = iter.next();
//			long id = e.getValue().getId();
//			int count = featCountMap.get(e.getKey());
//			sBuff.append(id).append(":").append(count).append(" ");
//		}
//		sBuff.append("\n");
//		try {
//			docWriter.write(sBuff.toString());
//		} catch (IOException e) {
//			addLogError("StreamingProcessor.writeOutDocument(): Problem writing doc to file: temp/docs.txt");
//			e.printStackTrace();
//		}
//	}
	
	private void writeOutDocument(Document doc, BufferedWriter docWriter, BufferedWriter docObjWriter)
	{
		docBuff.setLength(0);
		docBuff.append(doc.getFeatureCountMap().size()).append(" ");
		
		docObjBuff.setLength(0);
		
		Iterator<Entry<Feature,Integer>> iter = doc.getFeatureCountMap().entrySet().iterator();
		while(iter.hasNext())
		{
			Entry<Feature,Integer> e = iter.next();
			long id = e.getKey().getId();
			int count = e.getValue();
			docBuff.append(id).append(":").append(count).append(" ");
			

		}
		docBuff.append("\n");
		
		docObjBuff.append(doc.getId()).append(",").append(doc.getExternalId()).append(",")
				.append(dateFormat.format(doc.getTime())).append("\n");
		try {
			docWriter.write(docBuff.toString());
			docObjWriter.write(docObjBuff.toString());
		} catch (IOException e) {
			addLogError("StreamingProcessor.writeOutDocument(): Problem writing doc to file");
			e.printStackTrace();
		}
		
		//TODO: Write out document ids so they can be tracked
	}
	
	private void tokensToFeaturesAndDoc(List<String> tokenList, Map<String,Feature> featMap, Document doc)
	{
		featMap.clear();
//		featCountMap.clear();
		for (String token:tokenList)
		{
			Feature f = null;
			
			if (this.featureMap.containsKey(token))
			{
				f = featureMap.get(token);
			}
			if (!featMap.containsKey(token))
			{
				if (f== null)
				{
					f = new Feature();
//					f.setId(featureId++);    //Do later
					f.setName(token);
					f.setCount(1);
				}
				
				featMap.put(token, f);
				doc.addFeature(f);
//				featureMap.put(token, f);
				
			}else
			{
				if (f == null) f = featMap.get(token);
				doc.addFeatureCount(f, 1);
			}
			
			
//			//If feature is not in featMap (just for this doc), add it. If it is, increment count
//			if (!featMap.containsKey(token))
//			{
//				featMap.put(token, f);
//				doc.addFeature(f);
////				featCountMap.put(token,1);
//			}else
//			{
////				featCountMap.put(token, featCountMap.get(token) + 1);
//				doc.addFeatureCount(f, 1);
//			}
			
		}
	}
	
	public void addLoggable(Loggable loggable)
	{
		loggableList.add(loggable);
	}
	
	public void addLogOut(String entry)
	{
		System.out.println(entry);
		for (Loggable l:loggableList)
		{
			l.addLogOut(entry);
		}
	}
	
	public void addLogError(String entry)
	{
		System.err.println(entry);
		for (Loggable l:loggableList)
		{
			l.addLogError(entry);
		}
	}
	
	public List<Document> getDocs()
	{
		return docList;
	}
	
	private void mergeToFeatureMap(Map<String,Feature> featMap)
	{		
		Iterator<Entry<String,Feature>> iter = featMap.entrySet().iterator();
		//Can't do a putAll here because we need to update feature doc counts
		while(iter.hasNext())
		{
			Entry<String,Feature> e = iter.next();
			Feature f = e.getValue();
			
			
			if (!this.featureMap.containsKey(e.getKey())) {
				f.setId(featureId++);
				f.setCount(1);
				featureMap.put(e.getKey(), f);
			}else {
				featureMap.get(e.getKey()).addCount();
			}

		}
		
	}
	
	public static ArrayList<Feature> sortFeatureMap(Map<String,Feature> fMap)
	{
		ArrayList<Feature> sortedList = new ArrayList<Feature>(fMap.values());
		
		Comparator<Feature> featureSorter = new Comparator<Feature>(){
			public int compare(Feature f1, Feature f2)
			{
				boolean comp = f1.getId() >= f2.getId();
				return comp?1:-1;
			}
		};
		
		Collections.sort(sortedList,featureSorter);

		return sortedList;
	}
	
	public static void main(String[] args)
	{
		if (args.length == 0) 
		{
			System.err.println("Usage: java tm.preprocessor.PipelineProcessor <config Filename>");
			System.exit(1);
		}
		StreamingPreprocessor pp = new StreamingPreprocessor(args[0]);
		pp.processDocs();
		
	}
}
