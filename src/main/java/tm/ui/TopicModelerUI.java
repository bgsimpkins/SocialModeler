package tm.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import tm.topicmodel.LDAModelProcessor;
import tm.topicmodel.TopicModelProcessor;
import tm.utils.IOUtils;
import tm.web.FBScraperPanel;

public class TopicModelerUI extends JFrame implements ActionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2158479282657232468L;
	
	private ExploreUI exploreUI;
	private BuildUI buildUI;
	private TopicModelProcessor modelProcessor;
	private Properties props;
	
	private JTabbedPane tabPane;
	
	private String modelName = "";
	
	private File configFile;
	
	private TMDialog tmDialog;
	
	private JDialog scraperDialog;
	
	public TopicModelerUI()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1024,768);		
		
		initUI();
	}
	
	public void initUI()
	{
		tmDialog = new TMDialog();
		
		//Init task menu
		JMenuBar menuBar = new JMenuBar();
		
		JMenu menu = new JMenu("File");
		
		JMenuItem item = new JMenuItem("Open");
		item.addActionListener(this);
		menu.add(item);
		
		JMenu subMenu = new JMenu("Save");
		
		item = new JMenuItem("Config");
		item.setActionCommand("Save Config");
		item.addActionListener(this);
		subMenu.add(item);
		
		item = new JMenuItem("Topics");
		item.setActionCommand("Save Topics");
		item.addActionListener(this);
		subMenu.add(item);
		
		menu.add(subMenu);
		
		menuBar.add(menu);
		
		menu = new JMenu("Options");
		item = new JMenuItem("Garbage Collect");
		item.addActionListener(this);
		menu.add(item);
		
		menuBar.add(menu);
		
		menu = new JMenu("Tools");
		item = new JMenuItem("Facebook Scraper");
		item.addActionListener(this);
		menu.add(item);
		
		menuBar.add(menu);
		
		this.setJMenuBar(menuBar);
		
		
		tabPane = new JTabbedPane();
		
		buildUI = new BuildUI(this);
		tabPane.addTab("Build",buildUI);
		
		exploreUI = new ExploreUI(this);
		tabPane.addTab("Explore", exploreUI);
		
		getContentPane().add(tabPane);
		
		
		
		setVisible(true);
	}
	
	public TMDialog getTMDialog()
	{
		return tmDialog;
	}
	
	public void setFrameTitle(String title)
	{
		this.setTitle("TopicModeler- "+title);
	}
	

	public static void main(String[] args)
	{
		try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Error setting system look and feel!");
        }
		new TopicModelerUI();
	}
	
	public void openModelInExplore()
	{
		exploreUI.setProperties(props);
		
		String modelDirStr = props.getProperty("TopicModeler.TopicModel.Model.Dir").trim();
		if (!modelDirStr.endsWith("/")) modelDirStr += "/";
		
		//TODO: Get type of processor from props or something!!
		modelProcessor = new LDAModelProcessor(props);
		modelProcessor.addLoggable(buildUI);
		modelProcessor.loadModel();
		exploreUI.setTopicModelProcessor(modelProcessor);						
		exploreUI.init();
		buildUI.setTopicModelProcessor(modelProcessor);
		
		tabPane.setSelectedIndex(1);
//		String [] modelNameSplit = modelDirStr.split("/");
//		String modelName = modelNameSplit[modelNameSplit.length-1];
//		System.out.println("TopicModelerUI: Model name="+modelName);
//		this.modelName = modelName;
//		exploreUI.setModelName(modelName);
		setFrameTitle(modelProcessor.getModelName());
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getActionCommand().equals("Open"))
		{
			File file = IOUtils.showFileLoader(".","Open Properties File");
			configFile = file;
			props = IOUtils.loadPropsFile(file.getAbsolutePath());
			
			String modelDirStr = props.getProperty("TopicModeler.TopicModel.Model.Dir");		
			
			//TODO: Open props file in builder
			
			String[] modelFiles = new String[]{"wordProbs.csv","topicProbs.csv","docProbs.csv","docObjs.csv"};
			List<String> modelFileList = Arrays.asList(modelFiles);
			
			//If model seems to be open, offer user to open model in expore ui
			File modelDir = new File(modelDirStr);
			int foundCount  = 0;
			if (modelDir.isDirectory())
			{
				File[] files = modelDir.listFiles();
				
				for (File f:files)
				{
					if (modelFileList.contains(f.getName()))
					{
						foundCount++;
					}
				}
				
				if (foundCount != modelFileList.size())
				{
					
				}
				else
				{
					System.out.println("Built model found!!");
					int option = JOptionPane.showConfirmDialog(this, "It appears that a model has already been built for this properties file. Load?");
					if (option == JOptionPane.YES_OPTION)
					{
						openModelInExplore();
					}
				}
			}else
			{
				//TODO: Handle if not a directory!!!
			}
			
			buildUI.setProperties(props);
			buildUI.updateUIFromProps();
		}
		else if (e.getActionCommand().equals("Save Topics"))
		{
			exploreUI.saveTopics();
		}
		else if (e.getActionCommand().equals("Save Config"))
		{
			if (configFile == null)
				
			{
				
				configFile = IOUtils.showFileSaver(".","Save Config File");
			}
			props = new Properties();
			buildUI.setProperties(props);
			buildUI.updatePropsFromUI();
			buildUI.addLogOut("Writing config to file: "+configFile.getAbsolutePath());
			try {
				FileWriter fw = new FileWriter(configFile);
				props.store(fw, null);
				fw.close();
			} catch (IOException e1) {
				buildUI.addLogError("Error writing configuration to file!");
				e1.printStackTrace();
			}
		}
		else if (e.getActionCommand().equals("Garbage Collect"))
		{
			System.out.println("Running gc");
			System.gc();
		}
		else if (e.getActionCommand().equals("Facebook Scraper"))
		{
			if (scraperDialog == null)
			{
				scraperDialog = new JDialog();
				scraperDialog.setTitle("Facebook Scraper");
				//scraperDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				scraperDialog.setSize(500,400);
				
				FBScraperPanel fbs = new FBScraperPanel();
				scraperDialog.getContentPane().add(fbs);
				
				
			}
			scraperDialog.setVisible(true);
		}
		
	}
}
