package tm.web;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import tm.utils.IOUtils;

public class FBScraperPanel extends JPanel implements ActionListener
{
	private JTextPane instArea = new JTextPane();
	private JTextField atField = new JTextField();
	private JLabel varLabel = new JLabel("Enter FB ID:");
	private JTextField varField = new JTextField();
	
	private JLabel numScrapedLab, dateLab;
	
	private JButton startButton,stopButton;
	
	private FacebookScraper scraper;
	
	private JTextField outDirField = new JTextField();
	
	private JLabel scrapingLab = new JLabel("Scraping...");
	
	private static int refreshTime = 100;
	
	private long lastRefresh = System.currentTimeMillis();
	
	public FBScraperPanel()
	{
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Insets wideInsets = new Insets(5,5,5,5);
		Insets tightYInsets = new Insets(1,5,1,5);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.gridwidth = 2;	
		c.insets = wideInsets;
		instArea.setContentType("text/html");
		instArea.setText("You will need an access token from Facebook. " +
				"Go to <a href=\"https://developers.facebook.com/tools/explorer/\">https://developers.facebook.com/tools/explorer/</a>, " +
				"click on 'Get Access Token' and then 'Extended Permissions'. " +
				"Make sure 'Read Stream' is checked. You will have to log into Facebook. " +
				"Copy the access token into the field below."
				);
		instArea.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		instArea.setEditable(false);
		instArea.addHyperlinkListener(new HyperlinkListener()
		{
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) 
			{
				if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType())
				{
					Desktop f = Desktop.getDesktop();
					try {
						f.browse(e.getURL().toURI());
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (URISyntaxException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				
			}
			
		}
		);
		add(instArea,c);
		
		c.gridy=1;
		c.gridwidth=1;
		c.weightx = 0;
		JLabel atLabel = new JLabel("Access Token:");
		add(atLabel,c);
		
		c.weightx=1;
		c.gridx=1;
		add(atField,c);
		
		c.gridx=0;
		c.gridy=2;
		c.weightx=0;
		JLabel radioLab = new JLabel("Select Scraper:");
		add(radioLab,c);
		
		c.gridx=1;
		c.insets = tightYInsets;
		c.weightx=1;
		JRadioButton homeScrapeButton = new JRadioButton("Home Page Scrape");
		homeScrapeButton.addActionListener(this);
		add(homeScrapeButton,c);
		
		c.gridy=3;
		JRadioButton searchScrapeButton = new JRadioButton("Graph Search Scrape");
		searchScrapeButton.addActionListener(this);
		add(searchScrapeButton,c);
		
		ButtonGroup buttGroup = new ButtonGroup();
		buttGroup.add(homeScrapeButton);
		buttGroup.add(searchScrapeButton);
		homeScrapeButton.setSelected(true);
		
		c.gridx=0;
		c.gridy=4;
		c.weightx=0;
		add(varLabel,c);
		
		c.gridx=1;
		c.weightx=1;
		add(varField,c);
		
		c.gridx=0;
		c.gridy=5;
		c.weightx=0;
		c.insets = wideInsets;
		JLabel outDirLabel = new JLabel("Out Directory:");
		add(outDirLabel,c);
		
		JPanel outDirPanel = new JPanel();
		outDirPanel.setLayout(new GridLayout(1,2,25,25));
		outDirPanel.add(outDirField);
		
		JButton outDirSelect = new JButton("Browse");
		outDirSelect.addActionListener(this);
		outDirPanel.add(outDirSelect);
		
		c.gridx=1;
		c.weightx=1;
		add(outDirPanel,c);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1,2,50,50));
		//buttonPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		
		startButton = new JButton("Scrape");
		startButton.addActionListener(this);
		buttonPanel.add(startButton,c);
		
		stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		stopButton.addActionListener(this);
		buttonPanel.add(stopButton,c);
		
		c.insets = new Insets(15,5,10,5);
		c.gridwidth=2;
		c.gridx=0;
		c.gridy=6;
		c.weightx=0;
		add(buttonPanel,c);
		
		c.insets = wideInsets;
		c.gridwidth=1;
		c.gridx=0;
		c.gridy=7;
		scrapingLab.setForeground(Color.BLUE);
		scrapingLab.setVisible(false);
		add(scrapingLab,c);
		
		c.gridy=8;
		JLabel numScrapedLabLab = new JLabel("Messages Scraped:");
		add(numScrapedLabLab,c);
		
		c.gridx=1;
		c.weightx=1;
		numScrapedLab = new JLabel();
		add(numScrapedLab,c);
		
		c.gridx=0;
		c.gridy=9;
		c.weightx=0;
		JLabel dateLabLab = new JLabel("Current Date:");
		add(dateLabLab,c);
		
		c.gridx=1;
		c.weightx=1;
		dateLab = new JLabel();
		add(dateLab,c);
		
		scraper = new FacebookScraper();
		scraper.setScraperPanel(this);
		
		
	}
	
	public void updateScrapeData(long num, String dateStr)
	{
		if (System.currentTimeMillis()-lastRefresh >= refreshTime)
		{
			numScrapedLab.setText(""+num);
			dateLab.setText(dateStr);
			lastRefresh = System.currentTimeMillis();
		}
		
	}

	
	public void scrapeStopped()
	{
		scrapingLab.setVisible(false);
		stopButton.setEnabled(false);
		startButton.setEnabled(true);
	}
	
	public static void main(String[] args)
	{
		try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Error setting system look and feel!");
        }
		
		JFrame frame = new JFrame();
		frame.setTitle("Facebook Scraper");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500,400);
		
		FBScraperPanel fbs = new FBScraperPanel();
		frame.getContentPane().add(fbs);
		
		frame.setVisible(true);
		
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getActionCommand().equals("Home Page Scrape"))	
		{
			varLabel.setText("Enter FB ID:");
		}
		else if (e.getActionCommand().equals("Graph Search Scrape"))
		{
			varLabel.setText("Enter Search Query:");
		}
		else if (e.getActionCommand().equals("Scrape"))
		{
			startButton.setEnabled(false);
			stopButton.setEnabled(true);
			scrapingLab.setVisible(true);
			if (varLabel.getText().equals("Enter FB ID:"))
			{
				scraper.setMode(FacebookScraper.SCRAPE_HOME_PAGE);
				scraper.setId(varField.getText());
			}else if (varLabel.getText().equals("Enter Search Query:"))
			{
				scraper.setMode(FacebookScraper.SCRAPE_PUBLIC_SEARCH);
				scraper.setSearchString(varField.getText());
			}
			scraper.setOutDir(outDirField.getText());
			scraper.setToken(atField.getText());
			scraper.startScrape();
		}
		else if (e.getActionCommand().equals("Stop"))
		{
			scrapingLab.setVisible(false);
			scraper.interruptScrape();
		}
		else if (e.getActionCommand().equals("Browse"))
		{
			File file = IOUtils.showFileLoader(".", "Select Data Directory Name", true);
			outDirField.setText(file.getAbsolutePath());
		}
		
	}
}
