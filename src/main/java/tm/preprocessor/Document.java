package tm.preprocessor;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import tm.topicmodel.MetadataVariable;

public class Document 
{
	private int id;
	private long externalId;
	private Timestamp time;
	private HashMap<Feature,Integer> featureCountMap = new HashMap<Feature,Integer>();
	private HashMap<MetadataVariable,String> metadataMap= new HashMap<MetadataVariable,String>();
	
	public long getExternalId() {
		return externalId;
	}
	public void setExternalId(long id) {
		this.externalId = id;
	}
	
	public void setTime(Timestamp d)
	{
		time = d;
	}
	
	public Timestamp getTime()
	{
		return time;
	}
	public HashMap<Feature, Integer> getFeatureCountMap() {
		return featureCountMap;
	}
	public void addFeature(Feature f)
	{
		featureCountMap.put(f,1);
	}
	public void addFeatureCount(Feature f, int count)
	{
		featureCountMap.put(f, featureCountMap.get(f)+count);
	}
	
	public Integer removeFeature(Feature f)
	{
		return featureCountMap.remove(f);
	}
	
	public void removeFeatures(List<Feature> fList)
	{
		for (Feature f:fList)
		{
			featureCountMap.remove(f);
		}
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public void addMetadata(MetadataVariable md, String value)
	{
		metadataMap.put(md, value);
	}
	
	public String getMetadataVal(MetadataVariable var)
	{
		return metadataMap.get(var);
	}
}
