package tm.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;

import au.com.bytecode.opencsv.CSVReader;


public class IOUtils 
{
	public static Properties loadPropsFile(String propsFileName)
	{
		Properties props = new Properties();
		try
		{
			props.load(new FileInputStream(propsFileName));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		return props;
	}
	
	public static Connection initSimpleConnectionForSourceDB(Properties props)
	{
		Connection con = null;
		
		String URL = props.getProperty(
				"TopicModeler.Preprocessor.SourceDB.url");
		
		String user = props.getProperty(
				"TopicModeler.Preprocessor.SourceDB.user");
		
		String password = props.getProperty(
				"TopicModeler.Preprocessor.SourceDB.password");
		
		try {
			con = JDBCConnector.getConnection(URL, user, password);					
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return con;
	}
	
	public static String loadFileAsString(File file)
	{
		StringBuffer outBuff = new StringBuffer();
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			while((line=br.readLine()) != null)
			{
				outBuff.append(line).append("\n");
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			System.err.println("IOUtils.loadFileAsString(): Could not find file: "+file.getAbsolutePath());
			e.printStackTrace();
		}catch (IOException e) {
			System.err.println("IOUtils.loadFileAsString(): Could not load file: "+file.getAbsolutePath());
			e.printStackTrace();
		}
		return outBuff.toString();
	}
	
	public static List<String[]> loadCSV(File file)
	{
		List<String[]> rows = null;
		try {
			CSVReader reader = new CSVReader(new FileReader(file));
			rows = reader.readAll();
			reader.close();
		} catch (FileNotFoundException e) {
			System.err.println("IOUtils.loadCSV(): Could not find file: "+file.getAbsolutePath());
			e.printStackTrace();
		} catch (IOException e)
		{
			System.err.println("IOUtils.loadCSV(): Could not load file: "+file.getAbsolutePath());
			e.printStackTrace();
		}
		
		return rows;
	}
	
	public static Set<String> loadFileRowsAsSet(File file,boolean toLower)
	{
		HashSet<String> rowSet = new HashSet<String>();
		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			
			String line = "";
			
			while((line=br.readLine()) != null)
			{
				if (toLower) rowSet.add(line.toLowerCase());
				else rowSet.add(line);
			}
			
			br.close();
			
		} catch (FileNotFoundException e) {
			System.err.println("IOUtils.loadFileRowsAsSet: Could not find file: "+file.getAbsolutePath());
			e.printStackTrace();
		} catch (IOException e)
		{
			System.err.println("IOUtils.loadFileRowsAsSet(): Could not load file: "+file.getAbsolutePath());
			e.printStackTrace();
		}
		
		return rowSet;
	}
	
	public static File showFileLoader(String dir, String title)
	{
		return showFileLoader(dir,title,false);
	}
	
	public static File showFileLoader(String dir, String title, boolean dirsOnly)
	{
		File file = null;

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        
        if (dirsOnly )
        {
        	fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        else fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        File masterDir = new File(dir);

        if (masterDir.exists())
        {
            fc.setCurrentDirectory(masterDir);
        }
        else fc.setCurrentDirectory(new File("."));

        int returnVal = fc.showOpenDialog(null);
        if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            file = fc.getSelectedFile();
        }

        return file;
	}
	
	public static File showFileSaver(String dir, String title)
	{
		JFileChooser fc = new JFileChooser();

		File file = null;
		
		fc.setDialogTitle(title);
		
		File masterDir = new File(dir);

        if (masterDir.exists())
        {
            fc.setCurrentDirectory(masterDir);
        }
        else fc.setCurrentDirectory(new File("."));
		
		int returnVal = fc.showSaveDialog(null);
		
		if(returnVal == JFileChooser.APPROVE_OPTION)
        {
            file = fc.getSelectedFile();
        }

        return file;
	}
	
	public static void writeStringToFile(String s, String filename)
	{
		try {
			FileWriter fw  = new FileWriter(new File(filename));
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(s);
			bw.close();
		} catch (IOException e) {
			System.err.println("IOUtils.writeStringToFile(): Could not write file: "+filename);
			e.printStackTrace();
		}
	}
	
	/**Load metadata info. Key is the metadata variable name. Value is an array of length 2 that contains
	 * the filename and the type (timestamp or string are currently supported)
	 * 
	 * @param filename Path to file
	 * @return Map (key = metadata variable name, value=String[2]{filename,type})
	 */
	public static Map<String,String[]> loadMetadataInfo(String filename)
	{
		Map<String,String[]> mdInfo = new HashMap<String,String[]>();
		//columns: variable,file,type
		try {
			CSVReader reader = new CSVReader(new FileReader(new File(filename)));
			
			String[] line;
			
			//Skip header
			reader.readNext();
			
			while ((line=reader.readNext()) != null)
			{
				mdInfo.put(line[0], new String[]{line[1],line[2].toLowerCase()});
			}
			
			reader.close();
			
		} catch (FileNotFoundException e) 
		{
			System.err.println("IOUtils.loadMetadataInfo(): Couldn't find metadata info file: "+(filename));
			e.printStackTrace();
		} catch (IOException e)
		{
			System.err.println("IOUtils.loadMetadataInfo(): Couldn't read metadata info file: "+(filename));
			e.printStackTrace();
		}
		
		return mdInfo;
	}
	
	public static boolean childFileExists(File parent, String childRegex)
	{
		Pattern p = Pattern.compile(childRegex);
		
		File[] childFiles = parent.listFiles();
		for (File childFile:childFiles)
		{
			Matcher m = p.matcher(childFile.getName());
			if (m.matches()) return true;
		}
		
		return false;
	}
	
	public static void main(String[] args)
	{
		System.out.println(""+IOUtils.childFileExists(new File("/home/bsimpkins/Documents"), "AR?"));
	}
	
	public static void copyFile(File sourceFile, File destFile) throws IOException 
	{
		if (!destFile.exists()) 
		{
			destFile.createNewFile();
		}
		FileInputStream fIn = null;
		FileOutputStream fOut = null;
		FileChannel source = null;
		FileChannel destination = null;
		try {
			fIn = new FileInputStream(sourceFile);
			source = fIn.getChannel();
			fOut = new FileOutputStream(destFile);
			destination = fOut.getChannel();
			long transfered = 0;
			long bytes = source.size();
			while (transfered < bytes) 
			{
				transfered += destination.transferFrom(source, 0, source.size());
				destination.position(transfered);
			}
		} finally {
			if (source != null) {
				source.close();
			} else if (fIn != null) {
				fIn.close();
			}
			if (destination != null) {
				destination.close();
			} else if (fOut != null) {
				fOut.close();
			}
		}
	}
	
	public static void copyFolder(File src, File dest) throws IOException
	{
 
		if(src.isDirectory()){
 
    		//if directory not exists, create it
			if(!dest.exists()){
		    dest.mkdir();
			System.out.println("Directory copied from "  + src + "  to " + dest);
    		}
 
    		//list all the directory contents
    		String files[] = src.list();
 
    		for (String file : files) {
		    //construct the src and dest file structure
		    File srcFile = new File(src, file);
		    File destFile = new File(dest, file);
		    //recursive copy
		    copyFolder(srcFile,destFile);
		}
 
    	}else{
    		//if file, then copy it
    		//Use bytes stream to support all file types
    		InputStream in = new FileInputStream(src);
    	    OutputStream out = new FileOutputStream(dest); 
 
    	    byte[] buffer = new byte[1024];
 
	        int length;
	        //copy the file content in bytes 
	        while ((length = in.read(buffer)) > 0){
	    	   out.write(buffer, 0, length);
	        }
 
	        in.close();
	        out.close();
	        System.out.println("File copied from " + src + " to " + dest);
    	}
    }
}
