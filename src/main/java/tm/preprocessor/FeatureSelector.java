package tm.preprocessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tm.utils.IOUtils;

public class FeatureSelector 
{
	private Properties props;
	private Map<String,Feature> featureMap;
	private HashSet<String> stopwordSet = new HashSet<String>();
	private Pattern intPattern, decPattern;
	
	private static final String DECIMAL_PATTERN="(\\+|-)?([0-9]*(\\.[0-9]+))";
	private static final String INTEGER_PATTERN="-?\\d+";
	
	private StreamingPreprocessor pp;
	
	private boolean interrupt = false;
	
	public FeatureSelector(Properties props, StreamingPreprocessor pp)
	{
		this.props = props;
		this.pp = pp;
		
		intPattern = Pattern.compile(INTEGER_PATTERN);
		decPattern = Pattern.compile(DECIMAL_PATTERN);
		
		loadStopwords();
	}
	
	private void loadStopwords()	
	{
		String stopwordsDir = props.getProperty("TopicModeler.Preprocessor.FeatureSelector.StopWords.Dir");
		if (!stopwordsDir.endsWith("/")) stopwordsDir += "/";
		File swDir = new File(stopwordsDir);
		File[] swFiles = swDir.listFiles();
		for (File swFile:swFiles)
		{
			if (!swFile.isDirectory())
			{
				
				Set<String> stopWords = IOUtils.loadFileRowsAsSet(swFile,true);
				pp.addLogOut("Loaded stopwords from: "+swFile.getName()+" num="+stopWords.size());
				stopwordSet.addAll(stopWords);
			}
		}
	}
	
	public void setFeatures(Map<String,Feature> fMap)
	{
		this.featureMap = fMap;
	}
	
	public Map<String,Feature> removeStopWords(List<Feature> removedFeats)
	{
		removedFeats.clear();
//		pp.addLogOut("Before sw removal: "+featureMap.size());
		Iterator<Entry<String,Feature>> iter = featureMap.entrySet().iterator();
		while(iter.hasNext() && !interrupt)
		{
			Entry<String,Feature> e = iter.next();
			if (stopwordSet.contains(e.getKey())) {
				iter.remove();
				removedFeats.add(e.getValue());
			}
		}
		
//		pp.addLogOut("After sw removal: "+featureMap.size());
		return featureMap;
	}
	
	public Map<String,Feature> removeNumbers(List<Feature> removedFeats)
	{
		removedFeats.clear();
		Iterator<Entry<String,Feature>> iter = featureMap.entrySet().iterator();
		while(iter.hasNext() && !interrupt)
		{
			Entry<String,Feature> e = iter.next();
			
			//Remove integers
			Matcher m = intPattern.matcher(e.getKey());
			if (m.matches()) {
				iter.remove();
				removedFeats.add(e.getValue());
			}
			else{
				//Remove floating points
				m = decPattern.matcher(e.getKey());
				if (m.matches()) {
					iter.remove();
					removedFeats.add(e.getValue());
				}
			}
		}
		return featureMap;
	}
	
	public void interrupt()
	
	{
		interrupt = true;
	}
	
	public void removeMaxAndMinFeatDocsCounts(List<Document> docList)
	{
		pp.addLogOut("FeatureSelector.removeMaxAndMinFeatDocsCounts(): Removing features...");
		double min = Double.parseDouble(props.getProperty("TopicModeler.DocsPerFeature.Minimum"));
		double max = Double.parseDouble(props.getProperty("TopicModeler.DocsPerFeature.Maximum"));
		
		StringBuffer rBuff = new StringBuffer();
		int removeCount = 0;
		Iterator<Entry<String,Feature>> iter = featureMap.entrySet().iterator();
		while(iter.hasNext() && !interrupt)
		{
			Entry<String,Feature> e = iter.next();
			double prop = (double)e.getValue().getCount() / (double)docList.size();
			if (prop < min || prop > max) 
			{			
				
				for (Document doc:docList)
				{
					//if (doc.removeFeature(e.getValue()) != null) pp.addLogOut("Doc removed for feat:"+e.getValue().getId());
					doc.removeFeature(e.getValue());
				}
				iter.remove();
				removeCount++;
				rBuff.append(e.getValue().getId()).append(",").append(e.getKey()).append("\n");
			}
		}
		pp.addLogOut("FeatureSelector.removeMaxAndMinFeatDocsCounts(): "+removeCount+" features removed");
		//Update feature ids
		int featureId = 0;
		iter = featureMap.entrySet().iterator();
		while(iter.hasNext() && !interrupt)
		{
			Entry<String,Feature> e = iter.next();
			e.getValue().setId(featureId++);
		}
		if (interrupt) {
			return;
		}
		//Write removed features to file (mostly for debugging and checking)
		FileWriter fw;
		try {
			fw = new FileWriter(new File(StreamingPreprocessor.MODEL_DIR+"removedFeatures.txt"));
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(rBuff.toString());
			
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		pp.addLogOut("FeatureSelector.removeMaxAndMinFeatDocsCounts(): Done.");
	}
	
	public static void main(String[] args)
	{
		FeatureSelector fs = new FeatureSelector(IOUtils.loadPropsFile("src/test/resources/test.properties"),null);
		Map<String,Feature> fmap = new HashMap<String,Feature>();
		fmap.put("234", new Feature());
		fmap.put("23shit",new Feature());
		fmap.put("34444.44444",new Feature());
		fmap.put("balh albha blah",new Feature());
		fmap.put("0.95",new Feature());
		
		fs.setFeatures(fmap);
		fs.removeNumbers(new ArrayList<Feature>());
		
		Iterator<String> iter = fmap.keySet().iterator();
		while(iter.hasNext())
		{
			String key = iter.next();
			System.out.println(key);
		}
	}
}
