package tm.utils;

import java.util.Properties;

public class PropertyUtils 
{
	private static Properties props;

	public static void setProperties(Properties props)
	{
		PropertyUtils.props = props;
	}
	
	public static String getPropertyString(String key)
	{
		String prop = props.getProperty(key);
		
		//Return null if property is blank
		if (prop.trim().equals("")) return null;
		return prop.trim();
	}
	
	/**Returns the value of a boolean property. Returns false if value
	 * is missing or blank.
	 * @return Value of a boolean property, false if value
	 * is missing or blank.
	 * 
	 */
	public static boolean getPropertyBoolean(String key)
	{
		String propsStr = PropertyUtils.getPropertyString(key);
		if (propsStr == null) return false;
		else return Boolean.parseBoolean(propsStr);
		
	}
	
	public static double getPropertyDouble(String key)
	{
		String propsStr = PropertyUtils.getPropertyString(key);
		if (propsStr == null) return 0;
		else return Double.parseDouble(propsStr);
	}
}
