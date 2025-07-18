package tm.topicmodel;

import java.io.File;
import java.util.Properties;

import tm.utils.IOUtils;

public class FileDocumentQueryer implements DocumentQueryer
{
	private String docDir = "";
	private String docExtension = "txt";
	private String metadataDir = "metadata/";
//	private String metadataInfoFile = "info.csv";
	
	@Override
	public void init(Properties props) 
	{
		docDir = props.getProperty("TopicModeler.Preprocessor.SourceFile.Directory");
		if (!docDir.endsWith("/")) docDir += "/";
		
		docExtension = props.getProperty("TopicModeler.Preprocessor.SourceFile.Extension");
		if (!docExtension.startsWith(".")) docExtension = "."+docExtension;
		
		metadataDir = props.getProperty("TopicModeler.Preprocessor.SourceFile.Metadata.Directory");
		if (!metadataDir.endsWith("/")) metadataDir += "/";
		
//		metadataInfoFile = props.getProperty("TopicModeler.Preprocessor.SourceFile.Metadata.InfoFile");
	}

	@Override
	public String getDocumentContent(long docId) 
	{	
		String content= IOUtils.loadFileAsString(new File(docDir+docId+docExtension));
//		content = content.replace('\r', ' ');
//		content = content.replace('\n', ' ');
//		content = content.replace('\"','\'');
		
		return content;
	}

	@Override
	public Object getMetadata(long docId, String mdVarName)
	{
		return null;
	}
	
	@Override
	public void cleanUp() 
	{
		
	}

}
