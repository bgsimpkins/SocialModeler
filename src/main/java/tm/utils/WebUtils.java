package tm.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WebUtils 
{
	
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat FB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSSS");
	
	public static String getParameterFromURL(String url, String param)
	{
		String[] urlSpl = url.split("\\?");
		if (urlSpl.length != 2) return null;
		String[] paramSpl = urlSpl[1].split("&");
	
		for(String paramStr:paramSpl)
		{
			String[] valSpl = paramStr.split("=");
			if (valSpl[0].equals(param)) return valSpl[1];
		}
		
		return null;
	}
	
	public static String getLocalDateStringFromUTCString(String utcLongDateTime) 
	{
		
	    String localDateString = null;

	    long when = 0;
	    try {
	        when = FB_DATE_FORMAT.parse(utcLongDateTime).getTime();
	    } catch (ParseException e) {
	        e.printStackTrace();
	    }
	    localDateString = DATE_FORMAT.format(new Date(when + TimeZone.getDefault().getRawOffset() + (TimeZone.getDefault().inDaylightTime(new Date()) ? TimeZone.getDefault().getDSTSavings() : 0)));

	    return localDateString;
	}
}
