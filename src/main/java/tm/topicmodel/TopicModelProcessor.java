package tm.topicmodel;

import java.util.Date;
import java.util.List;
import java.util.Map;

import tm.preprocessor.Document;
import tm.ui.Loggable;

public interface TopicModelProcessor 
{ 
	/**Should return error code for model build*/
	public int buildModel();
	
	public void stopBuild();
	
	public void loadModel();
	
	public String getModelName();
	
	public List<ProbabilityValue> getTopFeatures(int topicId, int numFeats);

	public List<DocumentContent> getTopDocuments(int topicId, int numDocs);

	public Map<Integer,List<DocumentContent>> getAllTopDocuments(int numDocs);
	
	public List<DocumentContent> getTopDocumentsForTimeRange(int topicId, Date beginTime, Date endTime, int numDocs);
	
	public ProbabilityValue[] getAllTopicProbsForTimeRange(Date beginTime, Date endTime);

	public ProbabilityValue getTopicProbForTimeRange(int topicId, Date beginTime, Date endTime);
	 
	public Map<String,ProbabilityValue> getTopicProbsForTimeRangeAndMetadata(int topicId, Date beginTime, Date endTime,String metadataVar);
	 
	public Map<String,ProbabilityValue> getTopicProbsForTimeRangeAndMetadata(int topicId, Date beginTime, Date endTime,String metadataVar,List<String> metadataVals);
	
	public ProbabilityDistribution getTimeSeriesForTopic(int topicId, Date beginTime, Date endTime);
	
	public Map<String,ProbabilityDistribution> getTimeSeriesForTopicAndMetadata(int topicId, Date beginTime, Date endTime, String metadataVar);
	
	public Map<String, ProbabilityDistribution> getTimeSeriesForTopicAndMetadata(
			int topicId, Date beginTime, Date endTime, String metadataVar, List<String> metadataVals);
	
	public Map <String,MetadataVariable> getMetadataVariables();
	
	public List<ProbabilityValue> getTopMetaDataValuesForTopic(int topicId, MetadataVariable mdVar, int numValues);
	
	public int getNumTopics();

	public Document getDocument(long extId);
	
	public ProbabilityDistribution getTopicProbs();
	
	public DocumentQueryer getDocumentQueryer();
	
	/**Should return a 2-value array with min and max of time range
	 * 
	 * @return A 2-value array with min and max of time range
	 */
	public Date[] getTimeRange();
	
	public void addLoggable(Loggable loggable);
	
	public void addLogOut(String entry);
	
	public void addLogError(String entry);
	
	
}
