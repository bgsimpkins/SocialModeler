package tm.utils;

import java.awt.Color;

public class SimplePalette 
{
	public static Color[] getColors(int hueDiff, float saturation, float bright, int numColors)
	{
		Color[] cols = new Color[numColors];
		
		int hue = 0;
		for(int i=0; i < numColors; i++)
		{
			float hueFloat = (float)hue/360f;
			if (hueFloat > 1) hueFloat = hueFloat-1f;
			cols[i] = new Color(Color.HSBtoRGB(hueFloat, saturation,bright));
			
			hue+=hueDiff;
		}
		
		return cols;
	}
	
	public static void main(String[] args)
	{
		
	}
}
