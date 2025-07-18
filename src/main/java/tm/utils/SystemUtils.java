package tm.utils;

public class SystemUtils 
{
	public static String[] getEnvironmentPath()
	{
		String os = System.getProperty("os.name");
		String pathDelim = ":";
		if (os.startsWith("Windows"))
		{
			pathDelim = ";";
		}
		String [] pathSpl = System.getenv().get("PATH").split(pathDelim);
		if (pathSpl == null)
		{
			System.err.println("SystemUtils.getEnvironmentPath(): Could not get environment PATH variable values!!");
		}
		
		return pathSpl;
	}
}
