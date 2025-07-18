package tm.topicmodel;

import java.util.Properties;

public interface DocumentQueryer 
{
	public void init(Properties props);
	
	public String getDocumentContent(long docId);
	
	public Object getMetadata(long docId, String metadataVariable);
	
	public void cleanUp();
}
