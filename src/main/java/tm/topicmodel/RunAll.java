package tm.topicmodel;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import tm.preprocessor.StreamingPreprocessor;
import tm.utils.IOUtils;

public class RunAll 
{
	public static void main(String[] args)
	{
		if (args.length == 0) 
		{
			System.err.println("Usage: java tm.topicmodel.RunAll <config Filename>");
			System.exit(1);
		}
		
		Properties props = IOUtils.loadPropsFile(args[0]);
		String modelDir = props.getProperty("TopicModeler.TopicModel.Model.Dir");
		//Do preprocessing
		StreamingPreprocessor preprocessor = new StreamingPreprocessor(props);
		preprocessor.processDocs();
		
		LDAModelProcessor modelProcessor = new LDAModelProcessor(props);
		modelProcessor.buildModel();
//		modelProcessor.loadModel(modelDir);
		
		////Write top features for topics to CSV	
//		StringBuffer sBuff = new StringBuffer();
//		sBuff.append("topic_id,feature,prob\n");
//		int numTopics = modelProcessor.getNumTopics();
//		for (int i = 0; i < numTopics; i++)
//		{
//			List<ProbabilityValue> dist = modelProcessor.getTopFeatures(i, 20);
//			for (ProbabilityValue val:dist)
//			{
//				sBuff.append(i).append(",")
//					.append(val.getValue()).append(",")
//					.append(val.getProb()).append("\n");
//			}
//		}
//		IOUtils.writeStringToFile(sBuff.toString(), modelDir+"/test_topFeatures.csv");
//		
//		////Write top docs to CSV
//		sBuff = new StringBuffer();
//		sBuff.append("topic_id,doc_id,doc_content,prob\n");
//		
//
//		Map<Integer,List<DocumentContent>> dcMap = modelProcessor.getAllTopDocuments(15);
//		Iterator<Entry<Integer,List<DocumentContent>>> iter = dcMap.entrySet().iterator();
//		while(iter.hasNext())
//		{
//			Entry<Integer,List<DocumentContent>> e = iter.next();
//			for (DocumentContent dc:e.getValue())
//			{
//				sBuff.append(e.getKey()).append(",")
//						.append(dc.getDoc().getExternalId()).append(",\"")
//						.append(dc.getContent()).append("\",")
//						.append(dc.getProb()).append("\n");
//						
//			}
//		}
//			
//			
//		
//		IOUtils.writeStringToFile(sBuff.toString(), modelDir+"/test_topDocs.csv");
//		
//		////Write daily topic time series to CSV
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		
//		sBuff = new StringBuffer();
//		sBuff.append("date,topic_id,prob\n");
//		Calendar date = Calendar.getInstance();
//		try {
//			date.setTime(sdf.parse("2012-01-01"));
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		Calendar endDate = Calendar.getInstance();
//		try {
//			endDate.setTime(sdf.parse("2013-02-25"));
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		while(!date.after(endDate))
//		{
//			ProbabilityValue[] probVals = modelProcessor.getAllTopicProbsForTimeRange(date.getTime(), date.getTime());
//			for (ProbabilityValue pv:probVals)
//			{
//				sBuff.append(sdf.format(date.getTime())).append(",").append((Integer)pv.getValue()).append(",").append(pv.getProb()).append("\n");
//			}
//			date.add(Calendar.DAY_OF_MONTH, 1);
//		}
//		
//		IOUtils.writeStringToFile(sBuff.toString(), modelDir+"/test_topicTS.csv");
		
	}
}
