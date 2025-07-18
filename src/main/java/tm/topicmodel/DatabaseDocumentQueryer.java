package tm.topicmodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import tm.utils.IOUtils;

public class DatabaseDocumentQueryer implements DocumentQueryer
{
	private Connection conn;
	private PreparedStatement ps;
	
	@Override
	public void init (Properties props)
	{
		conn = IOUtils.initSimpleConnectionForSourceDB(props);
		String docQuery = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.Query");
		String docIdField = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.IdField");
		String whereFieldsAll = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.WhereValues");
		String[] whereFields = whereFieldsAll.split(",");
		
		String adnlClauses = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.AdditionalClauses");
		docQuery += " WHERE "+docIdField+"=?";
		for (String field:whereFields)
		{
			docQuery += " AND "+field;
		}
		docQuery += adnlClauses;

		try {
			ps = conn.prepareStatement(docQuery);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@Override
	public String getDocumentContent(long docId) 
	{
		String content = null;
		try {
			ps.setLong(1, docId);
			ResultSet rs = ps.executeQuery();
			rs.next();
			content = rs.getString(3);
			
			//Get rid of newlines, carriage returns, and replace double quotes with single quotes
//			content = content.replace('\r', ' ');
//			content = content.replace('\n', ' ');
//			content = content.replace('\"','\'');
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;
		
	}
	
	//TODO: Implement this!!
	public Object getMetadata(long docId, String mdVarName)
	{
		return null;
	}

	@Override
	public void cleanUp() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

}
