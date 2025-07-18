package tm.topicmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetadataVariable 
{
	private String varName = null;
	private String varType = null;
	private List<String> valueList= new ArrayList<String>();

	public MetadataVariable(String varName,String varType)
	{
		this.varName = varName;
		this.varType = varType;
	}
	
	public void addValue(String val)
	{
		valueList.add(val);
	}
	
	public List<String> getValues()
	{
		return valueList;
	}
	
	public void sortAscending()
	{
		Collections.sort(valueList, String.CASE_INSENSITIVE_ORDER);
	}
	
	public boolean containsValue(String val)
	{
		return valueList.contains(val);
	}
	
	public String getName()
	{
		return varName;
	}
	
	public String getType()
	{
		return varType;
	}
	
	@Override
	public String toString()
	{
		return varName;
	}
}
