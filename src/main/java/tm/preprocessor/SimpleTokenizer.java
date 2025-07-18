package tm.preprocessor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import tm.utils.IOUtils;

public class SimpleTokenizer 
{
	private Properties props;
	public SimpleTokenizer(Properties props)
	{
		this.props = props;
	}
	
	public void tokenize(String s, List<String> tokenList)
	{
		//This tokenList is likely used for repeated calcs. Clear
		tokenList.clear();
		
		//Batch remove everything, if desired (may be quicker than individual removal
		if (Boolean.parseBoolean(props.getProperty("TopicModeler.Preprocessor.SimpleTokenizer.BatchRemove")))
		{
			s = s.replaceAll("[!@#$%^&*(){}.,;:?<>=\\+\"'/\\\\]"," " );
//			s = s.replaceAll("[!@#$%^&*().,;:?<>=\\+\"']"," " );
			s = s.replace("--", " ");
		}
		else{

			if (Boolean.parseBoolean(props.getProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveQuotes")))
			{
				s = s.replace("\"", "");
				s = s.replace("'", "");
			}
			if (Boolean.parseBoolean(props.getProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveParens")))
			{
				s = s.replace("(", "");
				s = s.replace(")", "");
			}
			if (Boolean.parseBoolean(props.getProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveCommas")))
			{
				s = s.replace(",", "");
			}
			if (Boolean.parseBoolean(props.getProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveSentenceSeparators")))
			{
				s = s.replace(".", " ");
				s = s.replace(";", " ");
				s = s.replace("?", " ");
				s = s.replace("!", " ");
				s = s.replace(":", " ");
				s = s.replace("--", " ");
			}if (Boolean.parseBoolean(props.getProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveBreaks")))
			{
				s = s.replace("\n", " ");
				s = s.replace("\r", " ");
				s = s.replace("\t", " ");
			}	
		}
		s = s.toLowerCase();
		//Split on spaces
		String[] split = s.split("\\s+");
		Collections.addAll(tokenList, split);
	}
	
	//Simple test of tokenizer
	public static void main(String[] args)
	{
		ArrayList<String> tokenList = new ArrayList<String>();
		SimpleTokenizer st = new SimpleTokenizer(IOUtils.loadPropsFile("src/test/resources/test.properties"));
		String testDoc = IOUtils.loadFileAsString(new File("src/test/resources/testDoc.txt"));
		st.tokenize(testDoc, tokenList);
		for (int i = 0; i < tokenList.size(); i++)
		{
			System.out.println(tokenList.get(i));
		}
	}
}
