package tm.topicmodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import tm.preprocessor.Document;
import tm.ui.Loggable;
import tm.utils.IOUtils;
import au.com.bytecode.opencsv.CSVReader;

public class LDAModelProcessor implements TopicModelProcessor
{
	private Properties props;
	private ProbabilityDistribution[] featureProbs;
	private ProbabilityDistribution[] docProbs;
	private ProbabilityDistribution topicProbs;
	
	/**Key for docMap is external id*/
	private HashMap<Long,Document> docMap = new HashMap<Long,Document>();
	private int numTopics;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private SimpleDateFormat dateDayFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static String MODEL_DIR;
	public static long TIMESTEP;
	
	private Date[] timeRange;
	
	private DocumentQueryer docQueryer;
	
	private List<Loggable> loggableList = new ArrayList<Loggable>();
	
	private Process buildProcess = null;
	
	private Map <String,MetadataVariable> mdMap = new HashMap<String,MetadataVariable>();

	private String modelName = "";
	
	public LDAModelProcessor(Properties props)
	{
		this.props = props;
		MODEL_DIR = props.getProperty("TopicModeler.TopicModel.Model.Dir");
		if (!MODEL_DIR.endsWith("/")) MODEL_DIR += "/";
		numTopics = Integer.parseInt(props.getProperty("TopicModeler.TopicModel.K"));
		
		setupTimestep();
		
		setupDocQueryer();
		
		timeRange = new Date[2];
	}
	
	private void setupTimestep()
	{
		String timeStep = props.getProperty("TopicModeler.TopicModel.Timestep");
		if (timeStep.equalsIgnoreCase("day")) TIMESTEP=86400000;
		else if (timeStep.equalsIgnoreCase("hour")) TIMESTEP=3600000;
		else if (timeStep.equalsIgnoreCase("minute")) TIMESTEP=60000;
		else TIMESTEP = Long.parseLong(timeStep);
			
	}
	
	private void setupDocQueryer()
	{
		String docQueryerType = props.getProperty("TopicModeler.Preprocessor.DocumentSource");
		if (docQueryerType.toLowerCase().equals("db"))
		{
			docQueryer = new DatabaseDocumentQueryer();
		}
		else if (docQueryerType.toLowerCase().equals("file"))
		{
			docQueryer = new FileDocumentQueryer();
			
		}
		docQueryer.init(props);
	}
	
	public DocumentQueryer getDocumentQueryer()
	{
		return docQueryer;
	}
	
	@Override
	public int buildModel()
	{
		String rBinDir = props.getProperty("TopicModeler.Rhome.BinDir");
		
		//This can be empty (for Linux systems). If not, append / if necessary
		if (!rBinDir.equals("") && !rBinDir.endsWith("/")) rBinDir += "/";
		
		
		String alpha = props.getProperty("TopicModeler.TopicModel.alpha");
		String eta = props.getProperty("TopicModeler.TopicModel.eta");
		String nIter = props.getProperty("TopicModeler.TopicModel.nIter");
		String nBurnin = props.getProperty("TopicModeler.TopicModel.nBurnin");
		
		addLogOut("TopicModelProcessor.buildModel(): Running lda.collapsed.gibbs.sampler...");
		
		//TODO: needs fix for Mac and maybe others
		String executable = "Rscript";
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows"))
		{
			executable = "Rscript.exe";
		}else if (os.startsWith("Linux"))
		{
			executable = "Rscript";
		}else
		{
			addLogOut("Operating system not yet tested. Assuming it is a *nix variety (fingers crossed!)...");
		}
		//Create file objects so correct system-dependednt file separator is used
		File rBinDirFile = new File(rBinDir);
		if (rBinDir.equals("")) rBinDir = "";
		else rBinDir=rBinDirFile.getAbsolutePath()+File.separator;
		String modelDirUnix = MODEL_DIR.replace("\\", "/");
		
		String scriptString = null;
		//TODO: clean up this script string to be more flexible (e.g., handle spaces for rBinDir for linux, etc).
		if (os.startsWith("Windows")) scriptString = "\""+rBinDir+executable+"\""+" R"+File.separator+"doLDA.R \""+modelDirUnix+"\" "+numTopics+" "+alpha+" "+eta+" "+nIter+" "+nBurnin;
		else scriptString = rBinDir+executable+" R"+File.separator+"doLDA.R \""+modelDirUnix+"\" "+numTopics+" "+alpha+" "+eta+" "+nIter+" "+nBurnin;
		addLogOut("Call: "+scriptString);
		int exitVal = 1;
		
		Runtime rt = Runtime.getRuntime();
		try {
			buildProcess = rt.exec(scriptString);
			
			//Print output of R to console
			BufferedReader input = new BufferedReader(new InputStreamReader(buildProcess.getInputStream()));
			 
			String line=null;
			
			while((line=input.readLine()) != null) {
			    addLogOut(line);
			}
			exitVal = buildProcess.waitFor();
			addLogOut("TopicModelProcessor.buildModel(): Exited with error code "+exitVal);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Might as well auto generate this...
		writeOutNamedTopicsTemplate();
		
		//Load output probabilities for model
		loadFeatureProbs(MODEL_DIR+"wordProbs.csv");
		loadDocumentProbs(MODEL_DIR+"docProbs.csv",MODEL_DIR+"docObjs.csv");
		loadTopicProbs(MODEL_DIR+"topicProbs.csv");
		return exitVal;
	}
	
	@Override
	public void stopBuild()
	{
		addLogOut("LDAModelProcessor.stopBuild(): Destroying R build process..");
		buildProcess.destroy();
	}
	
//	private void saveModelToModelDir()
//	{
//		File modelDir = new File(props.getProperty("test.properties"));
//		
//		
//		try {
//			Path wpPath = FileSystems.getDefault().getPath("temp/wordProbs.csv");
//			FileOutputStream fos = new FileOutputStream(modelDir+"/wordProbs.csv");
//			Files.copy(wpPath,fos);
//			fos.close();
//			
//			wpPath = FileSystems.getDefault().getPath("temp/docProbs.csv");
//			fos= new FileOutputStream(modelDir+"/docProbs.csv");
//			Files.copy(wpPath,fos);
//			fos.close();
//			
//			
//		} catch (FileNotFoundException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	@Override
	public void loadModel()
	{
		String modelDir = props.getProperty("TopicModeler.TopicModel.Model.Dir").trim();
		
		String [] modelNameSplit = modelDir.split("/");
		modelName = modelNameSplit[modelNameSplit.length-1];
		
		if (!modelDir.endsWith("/")) modelDir += "/";
		
		addLogOut("LDAModelProcessor.loadModel(): Loading feature probs...");
		loadFeatureProbs(modelDir+"wordProbs.csv");
		
		addLogOut("LDAModelProcessor.loadModel(): Loading document probs...");
		loadDocumentProbs(modelDir+"docProbs.csv",modelDir+"docObjs.csv");
		
		addLogOut("LDAModelProcessor.loadModel(): Loading topic probs...");
		loadTopicProbs(modelDir+"topicProbs.csv");
		
		addLogOut("LDAModelProcessor.loadModel(): Loading metadata...");
		loadMetadata(modelDir);
	}
	
	private void loadFeatureProbs(String filename)
	{
		List<String[]> rows = null;
		try {
			CSVReader reader = new CSVReader(new FileReader(filename));
			 rows = reader.readAll();
			 reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Init dist list
		featureProbs = new ProbabilityDistribution[numTopics];
		for (int i = 0; i < numTopics; i++)
		{
			featureProbs[i] = new ProbabilityDistribution();
		}
		
		//Header, so start at line 1
		for (int i = 1; i < rows.size(); i++)
		{
			String[] row = rows.get(i);
			String value = row[0];
			
			for (int j = 1;j < row.length; j++)
			{
//				addLogOut("adding value "+row[j]);
				featureProbs[j-1].addValue(value, Double.parseDouble(row[j]));
			}
			
		}
		
	}
	
	
	
	private void writeOutNamedTopicsTemplate()
	{
		StringBuffer sBuff = new StringBuffer();
		String[] mDirSplit = MODEL_DIR.split("/");
		String modelName = mDirSplit[mDirSplit.length-1];
		sBuff.append("model,topic_id,topic_name,topic_desc\n");
		for (int i = 0; i < numTopics; i++)
		{
			sBuff.append("\"").append(modelName).append("\",").append(i).append(",,\n");
		}
		IOUtils.writeStringToFile(sBuff.toString(),MODEL_DIR+"topicsNamed.csv");
	}
	
	private void loadDocumentProbs(String docProbFilename, String docObjFilename)
	{
		List<String[]> rows = null;
		//SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		//Load doc objects first
		try{
			CSVReader reader = new CSVReader(new FileReader(docObjFilename));
			rows = reader.readAll();
			reader.close();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		long min = -1;//System.currentTimeMillis();
		long max = -1;
		for (String[] row:rows)
		{
			Document doc = new Document();
			doc.setId(Integer.parseInt(row[0]));
			doc.setExternalId(Long.parseLong(row[1]));
			try {
				doc.setTime(new Timestamp(dateFormat.parse(row[2]).getTime()));
				
			} catch (ParseException e) {
				try {
					doc.setTime(new Timestamp(dateDayFormat.parse(row[2]).getTime()));
				} catch (ParseException e1) {
					System.err.println("LDAModelProcessor.loadDocumentProbs(): Could not parse document timestamps!!");
				}
			}
			if (doc.getTime().getTime() < min || min == -1) min = doc.getTime().getTime();
			if (doc.getTime().getTime() > max || max == -1) max = doc.getTime().getTime();
			
			docMap.put(doc.getExternalId(),doc);
		}
		
		timeRange[0] = new Date(min);
		timeRange[1] = new Date(max);
		addLogOut("LDAModelProcessor.loadDocumentProbs(): Time range="+dateFormat.format(timeRange[0])+" to "+dateFormat.format(timeRange[1]));
		
		rows = null;
		try {
			CSVReader reader = new CSVReader(new FileReader(docProbFilename));
			 rows = reader.readAll();
			 reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Init dist list
		docProbs = new ProbabilityDistribution[numTopics];
		for (int i = 0; i < numTopics; i++)
		{
			docProbs[i] = new ProbabilityDistribution();
		}
		
		HashMap<Integer,Document> docIdMap = new HashMap<Integer,Document>();
		Iterator<Entry<Long,Document>> iter = docMap.entrySet().iterator();
		while(iter.hasNext())
		{
			Entry<Long,Document> e = iter.next();
			docIdMap.put(e.getValue().getId(), e.getValue());
		}
		
		
		//Header, so start at line 1
		for (int i = 1; i < rows.size(); i++)
		{
			String[] row = rows.get(i);
			for (int j = 0;j < row.length; j++)
			{
				docProbs[j].addValue(docIdMap.get(i),Double.parseDouble(row[j]));
			}
			
		}
		//Write out doc probs for debug
//		StringBuffer buff = new StringBuffer();
//		buff.append("id,extID,prob\n");
//		
//		List<ProbabilityValue> vals = docProbs[0].getDistributionValues();
//		
//		for (int i = 0; i < vals.size(); i++)
//		{
//			ProbabilityValue pv =vals.get(i);
//			Document doc = (Document)pv.getValue();
//			buff.append(doc.getId()).append(",").append(doc.getExternalId()).
//				append(",").append(pv.getProb()).append("\n");
//		}
//		IOUtils.writeStringToFile(buff.toString(), "testDocProbs.csv");
	}
	
	private void loadTopicProbs(String filename)
	{
		List<String[]> rows = null;
		try {
			CSVReader reader = new CSVReader(new FileReader(filename));
			rows = reader.readAll();
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		topicProbs = new ProbabilityDistribution();
		
		for (int i = 1; i < rows.size(); i++)
		{
			String[] row = rows.get(i);
			int id = Integer.parseInt(row[0]);
			double prob = Double.parseDouble(row[1]);
			topicProbs.addValue(id, prob);
		}
		
	}
	
	private void loadMetadata(String modelDir)
	{
		//Make hashmap for doc lookup by ext id
//		Iterator<Entry<Integer,Document>> dmIter = docMap.entrySet().iterator();
//		HashMap<Long,Document>docExtMap = new HashMap<Long,Document>();
//		while(dmIter.hasNext())
//		{
//			
//			Entry<Integer,Document> e = dmIter.next();
//			Document doc = e.getValue();
//			docExtMap.put(doc.getExternalId(), doc);
//		}
		
		String metadataDir = props.getProperty("TopicModeler.Preprocessor.SourceFile.Metadata.Directory");
		String infoFileName = props.getProperty("TopicModeler.Preprocessor.SourceFile.Metadata.InfoFile");
		Map<String,String[]> infoMap = IOUtils.loadMetadataInfo(modelDir+File.separator+metadataDir+File.separator+infoFileName);
		Iterator <Entry<String,String[]>> iter = infoMap.entrySet().iterator();
		int count = 0;
		while (iter.hasNext())
		{
			Entry<String,String[]> e = iter.next();
			String varName = e.getKey().toLowerCase();
			String varFile = e.getValue()[0].toLowerCase();
			String varType = e.getValue()[1].toLowerCase();
			
			
			//Ignore time variable
			if (varName.equals("time")) continue;
			MetadataVariable mdVar = new MetadataVariable(varName,varType);
			mdMap.put(varName,mdVar);
			if (!varType.equals("string"))
			{
				System.err.println("LDAModelProcessor.loadMetadata(): Metadata variable "+varName+" is of a type that " +
						"is not supported. Currently only string types are supported. Skipping this variable..");
				continue;
			}
			List<String[]> lines = IOUtils.loadCSV(new File(modelDir+File.separator+metadataDir+File.separator+varFile));
			for (String[] line:lines)
			{
				if (!mdVar.containsValue(line[1])) mdVar.addValue(line[1]);
				
				long docId = Long.parseLong(line[0]);
				Document doc = docMap.get(docId);
				if (doc == null)
				{
					System.err.println("LDAModelProcessor.loadMetadata(): Doc id "+docId+" not found in doc map when trying" +
							" to load metadata values for variable "+varName);
					continue;
				}
				count++;
				//System.out.println("Loading metadata for doc "+docId+": var="+varName+" val="+line[1]);
				doc.addMetadata(mdVar, line[1]);
			}
			mdVar.sortAscending();
 		}
		addLogOut("LDAModelProcessor.loadMetadata(): Loaded metadata for "+count+" docs!");
	}
	
	@Override
	public String getModelName()
	{
		return modelName;
	}
	
	@Override
	public Date[] getTimeRange()
	{
		return timeRange;		
	}
 	
	@Override
	public List<DocumentContent> getTopDocuments(int topicId, int numDocs)
	{

		//TODO: Add minimum probability to config to decrease the search and sorting time
		List<ProbabilityValue> probs = docProbs[topicId].sortDistDescending().subList(0, numDocs);	
		List<DocumentContent> dcList = new ArrayList<DocumentContent>();
	
		addLogOut("LDAModelProcessor.getTopDocuments(): Getting top docs for topic "+topicId);
		
		for (ProbabilityValue pv:probs)
		{
			Document doc = (Document)pv.getValue();
			String content = docQueryer.getDocumentContent(doc.getExternalId());
			DocumentContent dc = new DocumentContent();
			dc.setDoc(doc);
			dc.setContent(content);
			dc.setProb(pv.getProb());
			dcList.add(dc);
		}
		return dcList;
		
	}
	
	@Override
	public Map<Integer,List<DocumentContent>> getAllTopDocuments(int numDocs)
	{
		addLogOut("LDAModelProcessor.getAllTopDocuments(): Getting all top docs. Make take some time........");
		
		Map<Integer,List<DocumentContent>> tdMap = new HashMap<Integer,List<DocumentContent>>();
		
		for (int i = 0; i < numTopics; i++)
		{
			List<ProbabilityValue> probs = docProbs[i].sortDistDescending().subList(0, numDocs);
			
			List<DocumentContent> dcList = new ArrayList<DocumentContent>();
			
			
			for (ProbabilityValue pv:probs)
			{
				Document doc = (Document)pv.getValue();
				String content = docQueryer.getDocumentContent(doc.getExternalId());

				DocumentContent dc = new DocumentContent();
				dc.setDoc(doc);
				dc.setContent(content);
				dc.setProb(pv.getProb());
				dcList.add(dc);
			}	
			tdMap.put(i, dcList);
		}
		

		return tdMap;
		
	}
	
	@Override
	public List<DocumentContent> getTopDocumentsForTimeRange(int topicId, Date beginTime, Date endTime, int numDocs) 
	{
		
		List<ProbabilityValue> dpPVs  = docProbs[topicId].sortDistDescending();
		List<ProbabilityValue> pvs = new ArrayList<ProbabilityValue>();
		
		int count = 0;
		for (ProbabilityValue pv:dpPVs)
		{
			Document doc = (Document)pv.getValue();
			if (doc.getTime().getTime() >= beginTime.getTime() && doc.getTime().getTime() <= endTime.getTime())
			{
				pvs.add(pv);	
				if (++count >= numDocs) break;
			}
		}
		
		
		List<DocumentContent> dcList = new ArrayList<DocumentContent>();
		
		for (ProbabilityValue pv:pvs)
		{
			Document doc = (Document)pv.getValue();
			String content = docQueryer.getDocumentContent(doc.getExternalId());

			DocumentContent dc = new DocumentContent();
			dc.setDoc(doc);
			dc.setContent(content);
			dc.setProb(pv.getProb());
			dcList.add(dc);
		}

		
		return dcList;
	}
	
	@Override
	public ProbabilityValue[] getAllTopicProbsForTimeRange(Date beginTime, Date endTime)
	{
		addLogOut("LDAModelProcessor.getTopicProbsForTimeRange(): "+dateFormat.format(beginTime)+" to "+dateFormat.format(endTime));
		
		ProbabilityValue[] probSums = new ProbabilityValue[numTopics];
		
		for(int i = 0; i < probSums.length; i++)
		{
			probSums[i] = new ProbabilityValue();
			probSums[i].setValue(i);
		}
		
		//TODO: Don't need to iterate through each docProb (topic) distribution if distributions are first
		//made parallel by sorting by doc id
		for (int i = 0; i < docProbs.length; i++)
		{
			ProbabilityDistribution dp = docProbs[i];
			List<ProbabilityValue> pvList = dp.getDistributionValues();
			for (ProbabilityValue pv:pvList)
			{
				Document doc = (Document)pv.getValue();
				if (doc.getTime().getTime() >= beginTime.getTime() && doc.getTime().getTime() <= endTime.getTime())
				{
					probSums[i].setProb(probSums[i].getProb()+pv.getProb());
				}
			}
		}
		return probSums;
	}
	
	@Override
	public ProbabilityValue getTopicProbForTimeRange(int topicId, Date beginTime, Date endTime) 
	{
		ProbabilityValue topicProb = new ProbabilityValue();
		List<ProbabilityValue> pvList = docProbs[topicId].getDistributionValues();
		for (ProbabilityValue pv:pvList)
		{
			Document doc = (Document)pv.getValue();
			if (doc.getTime().getTime() >= beginTime.getTime() && doc.getTime().getTime() <= endTime.getTime())
			{
				topicProb.setValue(doc);
				topicProb.setProb(topicProb.getProb() + pv.getProb());
			}
			
		}
		
		// TODO Auto-generated method stub
		return topicProb;
	}
	
	public Map<String,ProbabilityValue> getTopicProbsForTimeRangeAndMetadata(int topicId, Date beginTime, Date endTime,String metadataVar)
	{
		//Get possible metadata values for this variable
		HashMap<String,ProbabilityValue> probMap = new HashMap<String,ProbabilityValue>();
		List<String> mdVals = mdMap.get(metadataVar).getValues();	

		//zero out probs
		Iterator<String> iter = mdVals.iterator();
		while(iter.hasNext())
		{
			String mdVal = iter.next();
			ProbabilityValue pv = new ProbabilityValue();
			pv.setProb(0);
			pv.setValue(mdVal);
			probMap.put(mdVal, pv);
		}
		
		//Go through doc probs. If within time range, check doc metadata against each value
		List<ProbabilityValue> pvList = docProbs[topicId].getDistributionValues();
		for (ProbabilityValue pv:pvList)
		{
			Document doc = (Document)pv.getValue();
			
			//Check if doc within time range
			if (doc.getTime().getTime() >= beginTime.getTime() && doc.getTime().getTime() <= endTime.getTime())
			{
				Iterator<String> iter2 = mdVals.iterator();
				while(iter2.hasNext())
				{
					String mdVal = iter2.next();
					String val = doc.getMetadataVal(mdMap.get(metadataVar));
					
					if (val != null && val.equals(mdVal))
					{
						ProbabilityValue mdPV = probMap.get(mdVal);
						mdPV.setProb(mdPV.getProb()+pv.getProb());
						mdPV.setValue(doc.getTime());
						System.out.println("Doc #"+doc.getExternalId()+"- Setting prob of "+mdVal+" to "+mdPV.getProb());
					}
				}
			}
		}
		
		return probMap;
	}
	
	@Override
	public Map<String,ProbabilityValue> getTopicProbsForTimeRangeAndMetadata(int topicId, Date beginTime, Date endTime,
			String metadataVar, List<String> metadataVals)
	{
		//Get possible metadata values for this variable
		HashMap<String,ProbabilityValue> probMap = new HashMap<String,ProbabilityValue>();
		List<String> mdVals = mdMap.get(metadataVar).getValues();	

		//zero out probs
		for (String metadataVal:metadataVals)
		{
			if (mdVals.contains(metadataVal))
			{
				ProbabilityValue pv = new ProbabilityValue();
				pv.setProb(0);
				pv.setValue(metadataVal);
				probMap.put(metadataVal, pv);
			}
		}
		
		//Go through doc probs. If within time range, check doc metadata against each value
		List<ProbabilityValue> pvList = docProbs[topicId].getDistributionValues();
		for (ProbabilityValue pv:pvList)
		{
			Document doc = (Document)pv.getValue();
			
			//Check if doc within time range
			if (doc.getTime().getTime() >= beginTime.getTime() && doc.getTime().getTime() <= endTime.getTime())
			{
				for (String mdVal:metadataVals)
				{
					String val = doc.getMetadataVal(mdMap.get(metadataVar));
					
					if (val != null && val.equals(mdVal))
					{
						ProbabilityValue mdPV = probMap.get(mdVal);
						mdPV.setProb(mdPV.getProb()+pv.getProb());
						mdPV.setValue(doc.getTime());
						///System.out.println("Doc #"+doc.getExternalId()+"- Setting prob of "+mdVal+" to "+mdPV.getProb());
					}
				}
			}
		}
		
		return probMap;
	}
	
	@Override
	public ProbabilityDistribution getTimeSeriesForTopic(int topicId, Date beginTime, Date endTime)
	{
		Date time = new Date(beginTime.getTime());
		
		ProbabilityDistribution dist = new ProbabilityDistribution();
		Date windowEnd = new Date(time.getTime()+TIMESTEP);
		
		while(windowEnd.getTime() <= endTime.getTime())
		{
			ProbabilityValue pv = getTopicProbForTimeRange(topicId, time, windowEnd);
			pv.setValue(new Date(time.getTime()));
			dist.addProbabilityValue(pv);
			
			time.setTime(time.getTime()+TIMESTEP);
			windowEnd.setTime(time.getTime()+TIMESTEP);
		}
		
		return dist;
	}
	
	public List<ProbabilityValue> getTopMetaDataValuesForTopic(int topicId, MetadataVariable mdVar, int numValues)
	{

		//TODO: Add minimum probability to config to decrease the search and sorting time	
		List<ProbabilityValue> mdValsTopic = new ArrayList<ProbabilityValue>();
		
		List<ProbabilityValue> pvList = docProbs[topicId].getDistributionValues();
		Map<String,ProbabilityValue> mdValMap = new HashMap<String,ProbabilityValue>();
		//Go through doc probs and find docs that are relevant to topic (less than minimum prob in config would
		//be better, but for now just not zero
		
		for (ProbabilityValue pv:pvList)
		{
			if (pv.getProb() == 0) continue;
			
			Document doc = (Document)pv.getValue();
			String mdVal = doc.getMetadataVal(mdVar);
			
			if (mdVal == null) continue;
			
			if (!mdValMap.containsKey(mdVal)) {
				ProbabilityValue pvNew = new ProbabilityValue();
				pvNew.setValue(mdVal);
				pvNew.setProb(pv.getProb());
				mdValMap.put(mdVal, pvNew);
			}
			else
			{
				ProbabilityValue pv1 = mdValMap.get(mdVal);
				pv1.setProb(pv1.getProb()+pv.getProb());
			}
		}
		
		ProbabilityDistribution mdDist = new ProbabilityDistribution();
		
		mdDist.addAllValues(mdValMap.values());
		
		if (numValues > mdDist.getDistributionValues().size()) numValues = mdDist.getDistributionValues().size();
		
		return mdDist.sortDistDescending().subList(0, numValues);
	}
	
	@Override
	public Map<String, ProbabilityDistribution> getTimeSeriesForTopicAndMetadata(
			int topicId, Date beginTime, Date endTime, String metadataVar) {
		
		Date time = new Date(beginTime.getTime());
		
		HashMap<String,ProbabilityDistribution> distMap = new HashMap<String,ProbabilityDistribution>();
		List<String> mdVals = mdMap.get(metadataVar).getValues();
		for(String mdVal:mdVals)
		{
			distMap.put(mdVal, new ProbabilityDistribution());
		}
		
		Date windowEnd = new Date(time.getTime()+TIMESTEP);
		
		while(windowEnd.getTime() <= endTime.getTime())
		{
			Map<String, ProbabilityValue> map = getTopicProbsForTimeRangeAndMetadata(topicId, time, windowEnd, metadataVar);
			Iterator<Entry<String,ProbabilityValue>> iter = map.entrySet().iterator();
			while(iter.hasNext())
			{
				Entry<String,ProbabilityValue> e = iter.next();
				ProbabilityValue pVal = e.getValue();
				
				ProbabilityDistribution dist = distMap.get(e.getKey());
				dist.addProbabilityValue(pVal);
			}
			
			time.setTime(time.getTime()+TIMESTEP);
			windowEnd.setTime(time.getTime()+TIMESTEP);
		}
		
		return distMap;
		
	}
	
	@Override
	public Map<String, ProbabilityDistribution> getTimeSeriesForTopicAndMetadata(
			int topicId, Date beginTime, Date endTime, String metadataVar, List<String> metadataVals) 
	{
		Date time = new Date(beginTime.getTime());
		
		HashMap<String,ProbabilityDistribution> distMap = new HashMap<String,ProbabilityDistribution>();
		List<String> mdVals = mdMap.get(metadataVar).getValues();
		for(String mdVal:mdVals)
		{
			distMap.put(mdVal, new ProbabilityDistribution());
		}
		
		Date windowEnd = new Date(time.getTime()+TIMESTEP);
		
		while(windowEnd.getTime() <= endTime.getTime())
		{
			Map<String, ProbabilityValue> map = getTopicProbsForTimeRangeAndMetadata(topicId, time, windowEnd, metadataVar,metadataVals);
			Iterator<Entry<String,ProbabilityValue>> iter = map.entrySet().iterator();
			while(iter.hasNext())
			{
				Entry<String,ProbabilityValue> e = iter.next();
				ProbabilityValue pVal = e.getValue();
				pVal.setValue(new Date(time.getTime()));
				
				ProbabilityDistribution dist = distMap.get(e.getKey());
				dist.addProbabilityValue(pVal);
			}
			
			time.setTime(time.getTime()+TIMESTEP);
			windowEnd.setTime(time.getTime()+TIMESTEP);
		}
		
		return distMap;
	}
	
	@Override
	public Map<String, MetadataVariable> getMetadataVariables() {
		return mdMap;
	}
	
	public Document getDocument(long extId)
	{
		return docMap.get(extId);
	}
	
	public ProbabilityDistribution getTopicProbs()
	{
		return topicProbs;
	}
	
	@Override
	public List<ProbabilityValue> getTopFeatures(int topicId, int numFeats)
	{
		return featureProbs[topicId].sortDistDescending().subList(0, numFeats);
	}

	
	@Override
	public int getNumTopics()
	{
		return numTopics;
	}

	@Override
	public void addLoggable(Loggable loggable) {
		loggableList.add(loggable);
		
	}

	@Override
	public void addLogOut(String entry) {
		System.out.println(entry);
		for (Loggable l:loggableList)
		{
			l.addLogOut(entry);
		}
		
	}

	@Override
	public void addLogError(String entry) {
		System.err.println(entry);
		for (Loggable l:loggableList)
		{
			l.addLogError(entry);
		}
		
	}

	

	
	
	
}
