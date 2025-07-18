package tm.utils;

import java.sql.Connection;
import java.sql.DriverManager;



public class JDBCConnector {

	public enum ConnectionType {MYSQL, SQLSERVER, POSTRGRES};
	
	public static ConnectionType connectionType;
	
	public static Connection getConnection(String URL, String user, String password) throws Exception {
		if (URL.contains(":mysql:")){
			System.out.println("MySQL Connection: " + URL);
			connectionType = ConnectionType.MYSQL;
			Class.forName("com.mysql.jdbc.Driver").newInstance();	
		}
		else if (URL.contains(":jtds:")){
			System.out.println("JTDS SQL Server Connection: " + URL);
			connectionType = ConnectionType.SQLSERVER;
			Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
		}
		else if (URL.contains(":sqlserver:")){
			System.out.println("SQL Server Connection: " + URL);
			connectionType = ConnectionType.SQLSERVER;
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();
		}
		
		else if (URL.contains(":postgresql:")){
			System.out.println("Postgres Connection: " + URL);
			connectionType = ConnectionType.POSTRGRES;
			Class.forName("org.postgresql.Driver").newInstance();
		}
		else{
			throw new Exception ("Unknown JDBC Driver");
		}
		Connection c;
		c = DriverManager.getConnection(URL,user,password);
		return c;
	}
}
