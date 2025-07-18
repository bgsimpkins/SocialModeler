package tm.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import tm.utils.IOUtils;
import tm.utils.WebUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FacebookScraper implements Runnable
{
	public static final int SCRAPE_HOME_PAGE=1;
	public static final int SCRAPE_PUBLIC_SEARCH=2;
	
	private int mode = SCRAPE_HOME_PAGE;
	
	private String token;
	private String id;
	
	private int limit = 10000;
	
	private SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private File outDir;
	
	private Map<Long,String> fbIDMap; 
	
	private FBScraperPanel scraperPanel;
	
	private boolean interrupt = false;
	
	private boolean continueDataOverlap = false;
	
	private String searchString;
	
	public void setMode(int scrapeMode)
	{
		this.mode = scrapeMode;
	}
	
	public void setSearchString(String ss)
	{
		this.searchString = ss;
	}
	
	public void interruptScrape()
	{
		interrupt = true;
	}
	
	public void setToken(String token)
	{
		this.token = token;
	}
	
	public void setScraperPanel(FBScraperPanel fbs)
	{
		this.scraperPanel = fbs;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public void setLimit(int limit)
	{
		this.limit = limit;
	}
	
	public int getLimit()
	{
		return limit;
	}
	
	public void setOutDir(File file)
	{
		outDir = file;
	}
	
	public void setOutDir(String filename)
	{
		outDir = new File(filename);
	}
	
	public File getOutDir()
	{
		return outDir;
	}
	
	@Override
	public void run()
	{
		interrupt = false;
		
		if(mode==SCRAPE_HOME_PAGE)
		{
			scrapeHomePage();			
		}
		else if (mode==SCRAPE_PUBLIC_SEARCH)
		{
			scrapeWithPublicSearch(searchString, 10000);
		}
	}
	
	public void startScrape()
	{
		Thread thread = new Thread(this);
		thread.start();
	}
	
	public void scrapeWithPublicSearch(String searchString, int pages)
	{
		if (!outDir.exists())
		{
			outDir.mkdir();
		}
		
		File mdDir = new File(outDir+"/metadata");
		if (!mdDir.exists()) mdDir.mkdir();
		File infoFile = new File(outDir+"/metadata/info.csv");
		
		if (!infoFile.exists())
		{
			try {
				
				FileWriter fw = new FileWriter(infoFile);
				String s = "variable,file,type\n"
						+  "time,time.csv,timestamp\n"
						+  "author,author.csv,string\n"
						+ "fbid,fbid.csv,string";
				fw.write(s);
				
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		File fbIdFile = new File(outDir+"/metadata/fbid.csv");
		boolean append = fbIdFile.exists();
		long docId = 1;
		
		//if fbIdFile exists, load it so we can check it for existing fbIDs
		if (append)
		{
			fbIDMap = new HashMap<Long,String>();
			List<String[]> rows = IOUtils.loadCSV(fbIdFile);
			for(String[] row:rows)
			{
				Long id = Long.parseLong(row[0]);
				fbIDMap.put(id,row[1]);
				
				if (id > docId) docId = id;
			}
			docId++;
		}
		
		FileWriter timeWriter = null;
		FileWriter authorWriter = null;
		FileWriter idWriter = null;
		try {
			timeWriter = new FileWriter(new File(outDir+"/metadata/time.csv"),append);		
			authorWriter = new FileWriter(new File(outDir+"/metadata/category.csv"),append);
			idWriter = new FileWriter(new File(outDir+"/metadata/fbid.csv"),append);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String urlString = "https://graph.facebook.com/search?fields=message,from&access_token="+
				token+"&q="+searchString+"&type=post";
		Date pageBeginTime = new Date(System.currentTimeMillis());
		String pageBeginTimeStr = "";
		long count = 0;
		for(int i = 0; i < pages; i++)
		{
			Home home = null;
			Object o = scrapeSinglePage(urlString,false);
			if (interrupt) break;
			home = (Home)o;

			pageBeginTimeStr = dayFormat.format(pageBeginTime);
			System.out.println("--Scraped page. Begin day="+pageBeginTimeStr);

			long oldDocId = docId;
			docId = writeOutMessages(home,docId,timeWriter,authorWriter,idWriter,false);
			
			//Increment count based on diff between old docId and new
			count += (docId-oldDocId);
			
			if (scraperPanel != null)
			{
				scraperPanel.updateScrapeData(count,pageBeginTimeStr);
			}
			
			if (interrupt) break;
			
			if (home==null) {
				System.out.println("No more pages...Breaking...");
				break;
			}
			
			Paging paging = home.getPaging();
			if (paging==null) {
				System.out.println("No more pages...Breaking...");
				
				break;
			}
			
			String nextUrl = paging.getNext();
			System.out.println("Next url: "+nextUrl);
			String startTimeStr = WebUtils.getParameterFromURL(nextUrl, "until");
			pageBeginTime.setTime(Long.parseLong(startTimeStr)*1000);
			
			urlString = nextUrl;
		}

		try {
			timeWriter.close();
			authorWriter.close();
			idWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (scraperPanel != null) {
			scraperPanel.scrapeStopped();
		}
	}
	
	public void scrapeHomePage()
	{
		//If model dir doesn't exist, make it
		if (!outDir.exists())
		{
			outDir.mkdir();
		}
		
		File mdDir = new File(outDir+"/metadata");
		if (!mdDir.exists()) mdDir.mkdir();
		File infoFile = new File(outDir+"/metadata/info.csv");
		
		if (!infoFile.exists())
		{
			try {
				
				FileWriter fw = new FileWriter(infoFile);
				String s = "variable,file,type\n"
						+  "time,time.csv,timestamp\n"
						+  "author,author.csv,string\n"
						+ "fbid,fbid.csv,string";
				fw.write(s);
				
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		File fbIdFile = new File(outDir+"/metadata/fbid.csv");
		boolean append = fbIdFile.exists();
		long docId = 1;
		
		//if fbIdFile exists, load it so we can check it for existing fbIDs
		if (append)
		{
			fbIDMap = new HashMap<Long,String>();
			List<String[]> rows = IOUtils.loadCSV(fbIdFile);
			for(String[] row:rows)
			{
				Long id = Long.parseLong(row[0]);
				fbIDMap.put(id,row[1]);
				
				if (id > docId) docId = id;
			}
			docId++;
		}

		FileWriter timeWriter = null;
		FileWriter authorWriter = null;
		FileWriter idWriter = null;
		try {
			timeWriter = new FileWriter(new File(outDir+"/metadata/time.csv"),append);		
			authorWriter = new FileWriter(new File(outDir+"/metadata/author.csv"),append);
			idWriter = new FileWriter(fbIdFile,append);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String urlString = "https://graph.facebook.com/"+id+"?access_token="+
				token+"&fields=home.limit("+limit+").fields(message,from)";
		Date pageBeginTime = new Date(System.currentTimeMillis());
		boolean firstPage = true;
		
		long count = 0;
		String pageBeginTimeStr = "";
		while(true)
		{
			Home home = null;
			Object o = scrapeSinglePage(urlString,firstPage);
			if (interrupt) break;
			if (firstPage) {
				Root root = (Root)o;
				home = root.getHome();
			}
			else
			{
				home = (Home)o;
			}
			pageBeginTimeStr = dayFormat.format(pageBeginTime);
			System.out.println("--Scraped page. Begin day="+dayFormat.format(pageBeginTime));
		
			long oldDocId = docId;
			docId = writeOutMessages(home,docId,timeWriter,authorWriter,idWriter,true);
			
			//Increment count based on diff between old docId and new
			count += (docId-oldDocId);
			
			if (scraperPanel != null)
			{
				scraperPanel.updateScrapeData(count,pageBeginTimeStr);
			}
			
			if (interrupt) break;
			
			if (home==null) {
				System.out.println("No more pages...Breaking...");
				break;
			}
			
			Paging paging = home.getPaging();
			if (paging==null) {
				System.out.println("No more pages...Breaking...");
				break;
			}
			
			String nextUrl = paging.getNext();
			System.out.println("Next url: "+nextUrl);
			String startTimeStr = WebUtils.getParameterFromURL(nextUrl, "until");
			pageBeginTime.setTime(Long.parseLong(startTimeStr)*1000);
			
			if (firstPage) firstPage = false;
			urlString = nextUrl;
		}

		try {
			timeWriter.close();
			authorWriter.close();
			idWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (scraperPanel != null) {
			scraperPanel.scrapeStopped();
		}
	}
	
	private long writeOutMessages(Home home, long docId, FileWriter timeWriter, FileWriter authorWriter, FileWriter idWriter,boolean writeAuthor)
	{
		List<Message> mList = home.getData();
		
		for(Message m:mList)
		{
			if (m.getMessage() == null) continue;
			
			if (fbIDMap != null && fbIDMap.containsValue(m.getId()) && !continueDataOverlap)
			{
				int resp = JOptionPane.showConfirmDialog(null, "Message id found in existing data (you likely " +
						"have the remaining messages already). Continue?"
						,"Data Overlap",JOptionPane.YES_NO_OPTION);
			
				if (resp == JOptionPane.YES_OPTION)
				{
					continueDataOverlap = true;
				}
				else
				{
					interrupt = true;
					break;
				}
			}
			
			File file = new File(outDir+"/"+docId+".txt");
			
			try {
				FileWriter fw = new FileWriter(file);		
				fw.write(m.getMessage());			
				fw.close();
				
				String dateFormatted = WebUtils.getLocalDateStringFromUTCString(m.getCreated_time());
				timeWriter.write(docId+",\""+dateFormatted+"\"\n");
				if (writeAuthor) authorWriter.write(docId+",\""+m.getFrom().getName()+"\"\n");
				else authorWriter.write(docId+",\""+m.getFrom().getCategory()+"\"\n");
				idWriter.write(docId+",\""+m.getId()+"\"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
			docId++;
		}
		
		return docId;
	}
	
	public Object scrapeSinglePage(String urlString,boolean root)
	{
		
		URL url;
		HttpURLConnection conn;
		BufferedReader br;
		String line;
		String theString = "";
		try
		{
			url = new URL(urlString);
			conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");
	        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        while ((line = br.readLine()) != null) 
	        {
	           theString += line;
	        }
	        br.close();
		}
		catch(IOException e)
		{
			JOptionPane.showMessageDialog(null, "IOException during scrape! You could be using a stale access token. Try a new one!");
			interrupt = true;
			//e.printStackTrace();
		}
		
		//System.out.println(theString);
		
		if (root) return rootJSONtoJava(theString);
		else return homeJSONtoJava(theString);
	}
	
	
	public Home homeJSONtoJava(String json)
	{
		Gson gson = new GsonBuilder().create();

		Home home = gson.fromJson(json, Home.class);
		
		return home;
	}
	
	public Root rootJSONtoJava(String json)
	{
		Gson gson = new GsonBuilder().create();

		Root root = gson.fromJson(json, Root.class);
		
		return root;
	}
	
//	public void scrapeHomePageToFile(int limit, String outFileName)
//	{
//		String urlString = "https://graph.facebook.com/"+id+"?access_token="+
//				token+"&fields=home.fields(message,from)";
//		URL url;
//		HttpURLConnection conn;
//		BufferedReader br;
//		BufferedWriter bw;
//		String line;
//		try
//		{
//			url = new URL(urlString);
//			conn = (HttpURLConnection) url.openConnection();
//	        conn.setRequestMethod("GET");
//	        br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//	        bw = new BufferedWriter(new FileWriter(new File(outFileName)));
//	        while ((line = br.readLine()) != null) 
//	        {
//	           bw.write(line+"\n");
//	        }
//	        br.close();
//	        bw.close();
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
	
	
	public static void main(String[] args)
	{
		
		FacebookScraper fbs = new FacebookScraper();
		fbs.setId("39514745");
		fbs.setToken("CAACEdEose0cBAD8CNrYDmczKaZCwyxE4YOsIaRUAgBZAyiJZBv3ZBqNwkY6D8kOUDkepNhDzLaR0BzY9vtQ2u0l1seElE1inzYGseFJQJeTMRQTQ44p0FiZB39VjTg4n5RScULXo1zLXPt94vu4WToqCn98EaYL6FcMxX0f0zhmZBDRbQSJKUJ94paNQHS8z8ZD");
		fbs.setOutDir("testFBPublicData");
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date d = null;
		try {
			d = sdf.parse("2013-10-01");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//fbs.scrapeHomePage(d);
		fbs.scrapeWithPublicSearch("mtsu", 2000);

		
	}
	
	class Root
	{
		private String id;
		public String getId() {
			return id;
		}
		public Home getHome() {
			return home;
		}
		private Home home;
	}
	class Home
	{
		private List<Message> data;
		public List<Message> getData() {
			return data;
		}
		public Paging getPaging() {
			return paging;
		}
		private Paging paging;
	}

	class Message
	{
		private From from;
		public From getFrom() {
			return from;
		}
		public String getMessage() {
			return message;
		}
		public String getId() {
			return id;
		}
		public String getCreated_time() {
			return created_time;
		}
		private String message;
		private String id;
		private String created_time;
	}

	class From
	{
		private String category;
		public String getCategory() {
			return category;
		}
		public String getName() {
			return name;
		}
		public long getId() {
			return id;
		}
		private String name;
		private long id;
	}

	class Paging
	{
		private String previous;
		public String getPrevious() {
			return previous;
		}
		public String getNext() {
			return next;
		}
		private String next;
	}

	
}


