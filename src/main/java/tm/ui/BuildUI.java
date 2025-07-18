package tm.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import tm.preprocessor.StreamingPreprocessor;
import tm.preprocessor.StreamingPreprocessorThread;
import tm.topicmodel.LDAModelProcessor;
import tm.topicmodel.LDAModelProcessorThread;
import tm.topicmodel.TopicModelProcessor;
import tm.utils.IOUtils;
import tm.utils.PropertyUtils;
import tm.utils.SystemUtils;
import au.com.bytecode.opencsv.CSVReader;

public class BuildUI extends JPanel implements ActionListener, Loggable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3062842523698962330L;

	private TopicModelProcessor modelProcessor;
	
	private Properties props = new Properties();
	
	private JTextPane consoleArea = new JTextPane();
    private StyledDocument doc;
    private Border standardConsoleBorder = BorderFactory.createLineBorder(Color.DARK_GRAY);
    private Border errorConsoleBorder = BorderFactory.createLineBorder(Color.RED);
	
	private final static String DBSOURCEPANEL = "DB";
	private final static String FILESOURCEPANEL = "Files";
	
	private JPanel sourceCards;
	
	private JTextField tsMillisField, urlField, userField, passField, fileDirField, mdDirField, extField,
			begTSField,endTSField,kField,alphaField,etaField,nIterField,nBurninField,stopWordsDirField,
			maxPropField, minPropField, modelDirField, rHomeField;
	
	private JCheckBox batchPunctBox, remQuotes, remEnclose, remCommas, remBreaks, remSentSeps, removeNumsBox;
	
	private JRadioButton dbRadio, fileRadio, tsDay, tsHour, tsMin, tsMillis;
	
	private JButton startPP,resetPP, startBuild,resetBuild;
	
	private StreamingPreprocessorThread spt;
	
	private TopicModelerUI tmUI;
	
	public BuildUI(TopicModelerUI tmUI)
	{	
		this.tmUI = tmUI;
		props = new Properties();
		PropertyUtils.setProperties(props);
		init();
	}
	
	public void setProperties(Properties props)
	{
		this.props = props;
		PropertyUtils.setProperties(props);
		//updateUIFromProps();
	}
	
	public void updateUIFromProps()
	{
		String source = props.getProperty("TopicModeler.Preprocessor.DocumentSource");
		
		if (source.toLowerCase().contains("file"))
		{
			fileRadio.setSelected(true);
		}
		else if (source.toLowerCase().contains("db"))
		{
			dbRadio.setSelected(true);
		}else 
		{
			int choice = JOptionPane.showConfirmDialog(this, "Document Source must be DB or files! Continue and default to files?", "Source Type Warning", JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.NO_OPTION) {
				System.err.println("ERROR: Exiting because of incorrect source type");
				System.exit(1);
			}
			
			//Default to file
			fileRadio.setSelected(true);
		}
		setTextFieldWithProperty("TopicModeler.Preprocessor.SourceDB.url",urlField);
		setTextFieldWithProperty("TopicModeler.Preprocessor.SourceDB.user",userField);
		setTextFieldWithProperty("TopicModeler.Preprocessor.SourceDB.password",passField);
		
		//TODO: Get query fields
		
		setTextFieldWithProperty("TopicModeler.Preprocessor.SourceFile.Directory",fileDirField);
		setTextFieldWithProperty("TopicModeler.Preprocessor.SourceFile.Metadata.Directory", mdDirField);
		setTextFieldWithProperty("TopicModeler.Preprocessor.SourceFile.Extension", extField);
		
		String timeRange = PropertyUtils.getPropertyString("TopicModeler.Preprocessor.SourceFile.Timerange");
		if (timeRange != null)		
		{
			String[] trSplit = timeRange.split(",");
			begTSField.setText(trSplit[0].trim());
			endTSField.setText(trSplit[1].trim());
		}
		else
		{
			//TODO: Handle no time range.
		}
		
		setCheckBoxWithProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveQuotes", remQuotes);
		setCheckBoxWithProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveParens", remEnclose);
		setCheckBoxWithProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveCommas", remCommas);
		setCheckBoxWithProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveSentenceSeparators", remSentSeps);
		setCheckBoxWithProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveBreaks", remBreaks);
		
		boolean batchRemove = PropertyUtils.getPropertyBoolean("TopicModeler.Preprocessor.SimpleTokenizer.BatchRemove");
		if (batchRemove)
		{
			batchPunctBox.setSelected(true);
			remQuotes.setEnabled(false);
			remBreaks.setEnabled(false);
			remCommas.setEnabled(false);
			remEnclose.setEnabled(false);
			remSentSeps.setEnabled(false);
		}
		
		setCheckBoxWithProperty("TopicModeler.Preprocessor.FeatureSelector.RemoveNumbers",removeNumsBox);
		setTextFieldWithProperty("TopicModeler.Preprocessor.FeatureSelector.StopWords.Dir",stopWordsDirField);
		
		setTextFieldWithProperty("TopicModeler.DocsPerFeature.Minimum", minPropField);
		setTextFieldWithProperty("TopicModeler.DocsPerFeature.Maximum", maxPropField);
	
		setTextFieldWithProperty("TopicModeler.TopicModel.Model.Dir",modelDirField);
		
		setTextFieldWithProperty("TopicModeler.Rhome.BinDir", rHomeField);
		
		String timeStep = PropertyUtils.getPropertyString("TopicModeler.TopicModel.Timestep");
		if (timeStep.toLowerCase().equals("day")) tsDay.setSelected(true);
		else if (timeStep.toLowerCase().equals("hour")) tsHour.setSelected(true);
		else if (timeStep.toLowerCase().contains("min")) tsMin.setSelected(true);
		else
		{
			tsMillis.setSelected(true);
			tsMillisField.setText(timeStep);
			tsMillisField.setEnabled(true);
		}
		
		setTextFieldWithProperty("TopicModeler.TopicModel.K", kField);
		setTextFieldWithProperty("TopicModeler.TopicModel.alpha", alphaField);
		setTextFieldWithProperty("TopicModeler.TopicModel.eta", etaField);
		setTextFieldWithProperty("TopicModeler.TopicModel.nIter", nIterField);
		setTextFieldWithProperty("TopicModeler.TopicModel.nBurnin", nBurninField);
		
	}
	
	private void setTextFieldWithProperty(String key,JTextField field)
	{
		String s = PropertyUtils.getPropertyString(key);
		if (s != null)
		{
			field.setText(s);
		}
	}
	
	public TopicModelProcessor getTopicModelProcessor() {
		return modelProcessor;
	}

	public void setTopicModelProcessor(TopicModelProcessor modelProcessor) {
		this.modelProcessor = modelProcessor;
	}
	
	public void updatePropsFromUI()
	{
		addLogOut("BuildUI: Updating props from UI...");
		if (dbRadio.isSelected()) props.setProperty("TopicModeler.Preprocessor.DocumentSource", "db");
		else if (fileRadio.isSelected()) props.setProperty("TopicModeler.Preprocessor.DocumentSource", "file");
		
		props.setProperty("TopicModeler.Preprocessor.SourceDB.url", urlField.getText().trim());
		props.setProperty("TopicModeler.Preprocessor.SourceDB.user", userField.getText().trim());
		props.setProperty("TopicModeler.Preprocessor.SourceDB.password", passField.getText().trim());
	
		//TODO: Update with db query fields
		
		props.setProperty("TopicModeler.Preprocessor.SourceFile.Directory", fileDirField.getText().trim());
		props.setProperty("TopicModeler.Preprocessor.SourceFile.Extension", extField.getText().trim());
		props.setProperty("TopicModeler.Preprocessor.SourceFile.Metadata.Directory", mdDirField.getText().trim());
		
		//TODO: Should probably add a field for this	
		props.setProperty("TopicModeler.Preprocessor.SourceFile.Metadata.InfoFile", "info.csv");
		
		props.setProperty("TopicModeler.Preprocessor.SourceFile.Timerange", begTSField.getText().trim()+","+endTSField.getText().trim());
		//TODO: Check format of datetimes using SimpleDateFormat (catch ParseException)
		
		props.setProperty("TopicModeler.Preprocessor.SimpleTokenizer.BatchRemove", batchPunctBox.isSelected()+"");
		props.setProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveQuotes", remQuotes.isSelected()+"");
		props.setProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveParens", remEnclose.isSelected()+"");
		props.setProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveCommas", remCommas.isSelected()+"");
		props.setProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveSentenceSeparators", remSentSeps.isSelected()+"");
		props.setProperty("TopicModeler.Preprocessor.SimpleTokenizer.RemoveBreaks", remBreaks.isSelected()+"");
		
		props.setProperty("TopicModeler.Preprocessor.FeatureSelector.RemoveNumbers", removeNumsBox.isSelected()+"");
		
		props.setProperty("TopicModeler.Preprocessor.FeatureSelector.StopWords.Dir", stopWordsDirField.getText().trim());
		props.setProperty("TopicModeler.DocsPerFeature.Minimum", minPropField.getText().trim());
		props.setProperty("TopicModeler.DocsPerFeature.Maximum", maxPropField.getText().trim());
		
		props.setProperty("TopicModeler.Rhome.BinDir", rHomeField.getText().trim());
		props.setProperty("TopicModeler.TopicModel.Model.Dir", modelDirField.getText().trim());
		
		if (tsDay.isSelected()) props.setProperty("TopicModeler.TopicModel.Timestep", "day");
		else if (tsHour.isSelected()) props.setProperty("TopicModeler.TopicModel.Timestep", "hour");
		else if (tsMin.isSelected()) props.setProperty("TopicModeler.TopicModel.Timestep", "minute");
		else 
		{
			props.setProperty("TopicModeler.TopicModel.Timestep", tsMillisField.getText().trim());
		}
	
		props.setProperty("TopicModeler.TopicModel.K", kField.getText().trim());
		props.setProperty("TopicModeler.TopicModel.alpha", alphaField.getText().trim());
		props.setProperty("TopicModeler.TopicModel.eta",etaField.getText().trim());
		props.setProperty("TopicModeler.TopicModel.nIter",nIterField.getText().trim());
		props.setProperty("TopicModeler.TopicModel.nBurnin",nBurninField.getText().trim());
		
	}
	
	private void setCheckBoxWithProperty(String key, JCheckBox box)
	{
		boolean b = PropertyUtils.getPropertyBoolean(key);
		box.setSelected(b);
	}
	
	private void init()
	{
		setLayout(new GridLayout(1,1));
		
		JPanel leftPanel = new JPanel();
		leftPanel.setMinimumSize(new Dimension(450,300));
		leftPanel.setPreferredSize(new Dimension(500,300));
		
		leftPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,5,5,5);
		c.anchor = GridBagConstraints.CENTER;
		c.weightx =1;
		
		JLabel ppLabel = new JLabel("Preprocessor");
		ppLabel.setFont(new Font("sanserif",Font.BOLD,18));
		ppLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		c.weighty=0;
		leftPanel.add(ppLabel,c);
		
		c.fill = GridBagConstraints.BOTH;
		c.weighty = .8;
		c.gridy=1;
		leftPanel.add(makeSourcePanel(),c);
		
		c.gridy=2; 
		c.weighty = .8;
		leftPanel.add(makeTokenizerPanel(),c);
		
		c.gridy=3; 
		c.weighty = .8;
		leftPanel.add(makeFeatureSelectorPanel(),c);
		
		JPanel ppButtonPanel = new JPanel();
		ppButtonPanel.setLayout(new GridBagLayout());
		GridBagConstraints ppC = new GridBagConstraints();
		ppC.insets = new Insets(0,10,0,10);
		
		startPP = new JButton("Start");
		startPP.setActionCommand("Start Preprocessing");
		startPP.addActionListener(this);
		ppButtonPanel.add(startPP,ppC);
		
		ppC.gridx = 1;
		
		resetPP = new JButton("Stop");
		resetPP.setActionCommand("Stop Preprocessing");
		resetPP.setEnabled(false);
		resetPP.addActionListener(this);
		ppButtonPanel.add(resetPP,ppC);
		
		c.gridy=4; 
		c.weighty = .4;
		leftPanel.add(ppButtonPanel,c);
		
		JPanel rightPanel = new JPanel();
		rightPanel.setMinimumSize(new Dimension(300,300));
		rightPanel.setPreferredSize(new Dimension(300,300));
		rightPanel.setLayout(new GridBagLayout());
		c = new GridBagConstraints();
//		c.fill = GridBagConstraints.BOTH;
		
		
		c.weighty=0;
		c.insets = new Insets(5,5,0,5);
		
//		c.gridwidth=2;
		JLabel bLabel = new JLabel("Build");
		bLabel.setFont(new Font("sanserif",Font.BOLD,18));
		bLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		
		
		rightPanel.add(bLabel,c);
		
		
		c.weightx=1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
//		c.gridwidth=1;
		c.gridy=1;
		rightPanel.add(makeBuildPanel(),c);
		
		JPanel buildButtonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints bc = new GridBagConstraints();
		bc.insets = new Insets(5,10,10,10);
		
		startBuild = new JButton("Start");
		startBuild.setActionCommand("Start Build");
		startBuild.addActionListener(this);
		buildButtonPanel.add(startBuild,bc);
		
		bc.gridx=1;
		resetBuild = new JButton("Stop");
		resetBuild.setActionCommand("Stop Build");
		resetBuild.addActionListener(this);
		resetBuild.setEnabled(false);
		buildButtonPanel.add(resetBuild,bc);
		
		c.gridy = 2;
		rightPanel.add(buildButtonPanel,c);
		
		c.gridy = 3;
		c.weighty=1;
//		consoleArea.setWrapStyleWord(true);
//		consoleArea.setLineWrap(true);

		consoleArea.setEditable(false);
		consoleArea.setBorder(standardConsoleBorder);
		doc = consoleArea.getStyledDocument();
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		Style font1 = doc.addStyle("font1_black", def);
		StyleConstants.setForeground(font1, Color.BLACK);
		
		Style font2 = doc.addStyle("font1_red", def);
		StyleConstants.setForeground(font2, Color.RED);
		
		rightPanel.add(new JScrollPane(consoleArea),c);
		
		JSplitPane lrSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,leftPanel,rightPanel);
		add(lrSplit);

	}
	
	private JPanel makeSourcePanel()
	{
		
		Dimension fieldSize = new Dimension(100,22);
		Dimension minFieldSize = new Dimension(70,22);
		
		JPanel dbSourcePanel = new JPanel();
		dbSourcePanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;	
//		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		c.insets = new Insets(3,5,3,10);
		
		
		c.gridx = 0;
		c.gridy = 1;
		JLabel urlLab = new JLabel("Url:");
		dbSourcePanel.add(urlLab,c);
		
		c.gridx= 1;
		c.fill = GridBagConstraints.BOTH;
		urlField = new JTextField();
		urlField.setPreferredSize(fieldSize);
		urlField.setMinimumSize(minFieldSize);
		dbSourcePanel.add(urlField,c);		
		
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.NONE;
		JLabel userLab = new JLabel("User:");
		dbSourcePanel.add(userLab,c);
		
		c.gridx = 1;
		c.weightx = .8;
		c.fill = GridBagConstraints.BOTH;
		userField = new JTextField();
		userField.setPreferredSize(fieldSize);
		userField.setMinimumSize(minFieldSize);
		dbSourcePanel.add(userField,c);
		
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		JLabel passLab = new JLabel("Password:");
		dbSourcePanel.add(passLab,c);
		
		c.gridx = 1;
		c.weightx = .8;
		c.fill = GridBagConstraints.BOTH;
		passField = new JTextField();
		passField.setPreferredSize(fieldSize);
		passField.setMinimumSize(minFieldSize);
		dbSourcePanel.add(passField,c);
		
		JPanel fileSourcePanel = new JPanel(new GridBagLayout());
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;	
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		c.weightx = 0;
		c.insets = new Insets(3,5,3,10);
		
		JLabel fileDirLabel = new JLabel("File Dir:");
		fileSourcePanel.add(fileDirLabel,c);
		
		c.gridx=1;
		c.weightx = 1;
		fileDirField = new JTextField();
		fileDirField.setPreferredSize(fieldSize);
		fileDirField.setMinimumSize(minFieldSize);
		fileSourcePanel.add(fileDirField,c);
		
		c.gridx=2;
		c.weightx=0;
		JButton browse = new JButton("Browse");
		browse.setMinimumSize(new Dimension(10,22));
		browse.setPreferredSize(new Dimension(100,22));
		browse.setMaximumSize(new Dimension(130,22));
		browse.setActionCommand("Browse Source Files Dir");
		browse.addActionListener(this);
		fileSourcePanel.add(browse,c);
		
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		JLabel mdDirLabel = new JLabel("Metadata dir (in File dir):");
		fileSourcePanel.add(mdDirLabel,c);
		
		c.gridx = 1;
		c.weightx = 1;
		c.gridwidth=2;
		mdDirField = new JTextField();
		mdDirField.setPreferredSize(fieldSize);
		mdDirField.setMinimumSize(minFieldSize);
		fileSourcePanel.add(mdDirField,c);
		
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0;
		c.gridwidth=1;
		JLabel extLabel = new JLabel("Source File Extension:");
		fileSourcePanel.add(extLabel,c);
		
		c.gridx = 1;
		c.weightx = 1;
		c.gridwidth=2;
		extField = new JTextField();
		extField.setPreferredSize(fieldSize);
		extField.setMinimumSize(minFieldSize);
		fileSourcePanel.add(extField,c);
		
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 0;
		c.gridwidth=1;
		JLabel timeStepLabel = new JLabel("Time Range:");
		fileSourcePanel.add(timeStepLabel,c);
		
		c.gridy = 4;
		JLabel begTSLabel = new JLabel("Begin (yyyy-MM-dd):");
		fileSourcePanel.add(begTSLabel,c);
		
		c.gridx = 1;
		c.weightx = 1;
		c.gridwidth=2;
		begTSField = new JTextField();
		begTSField.setPreferredSize(fieldSize);
		begTSField.setMinimumSize(minFieldSize);
		fileSourcePanel.add(begTSField,c);
		
		c.gridx = 0;
		c.gridy = 5;
		c.weightx = 0;
		c.gridwidth=1;
		JLabel endTSLabel = new JLabel("End (yyyy-MM-dd):");
		fileSourcePanel.add(endTSLabel,c);
		
		c.gridx = 1;
		c.weightx = 1;
		c.gridwidth=2;
		endTSField = new JTextField();
		endTSField.setPreferredSize(fieldSize);
		endTSField.setMinimumSize(minFieldSize);
		fileSourcePanel.add(endTSField,c);
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;	
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		c.insets = new Insets(3,5,3,10);
		
		JPanel sourceSelectPanel = new JPanel();	
		sourceSelectPanel.setLayout(new GridBagLayout());
		c = new GridBagConstraints();
		c.insets = new Insets(3,5,3,10);
		dbRadio = new JRadioButton("DB");
		dbRadio.setActionCommand("Show DB Source Panel");
		dbRadio.addActionListener(this);
		fileRadio = new JRadioButton("Files");
		fileRadio.setActionCommand("Show File Source Panel");
		fileRadio.addActionListener(this);
		sourceSelectPanel.add(fileRadio,c);
		
		c.gridx = 1;
		sourceSelectPanel.add(dbRadio,c);
		fileRadio.setSelected(true);
		ButtonGroup bg = new ButtonGroup();
		bg.add(dbRadio);
		bg.add(fileRadio);
//		c.gridx = 2;
		
		sourceCards = new JPanel(new CardLayout());
		sourceCards.add(fileSourcePanel,FILESOURCEPANEL);
		sourceCards.add(dbSourcePanel,DBSOURCEPANEL);
				
		JPanel sourcePanel = new JPanel(new GridBagLayout());
		sourcePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Source"));
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;	
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		c.weightx = 1;
		sourcePanel.add(sourceSelectPanel,c);
		
		c.gridy = 1;
		sourcePanel.add(sourceCards,c);
		
		return sourcePanel;
	}
	
	private JPanel makeTokenizerPanel()
	{
		JPanel tokPanel = new JPanel();
		
		tokPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Tokenizer"));;
		tokPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.WEST;
//		c.weighty = .2;
		c.weightx = 1;
		c.insets = new Insets(3,5,3,10);
		c.gridwidth=3;
//		JLabel tokenLab = new JLabel("Tokenizer:");
//		tokenLab.setFont(new Font("sanserif",Font.BOLD,14));
//		tokPanel.add(tokenLab,c);
		

		c.gridy = 1;
		c.gridx = 1;
		c.gridwidth=1;
		batchPunctBox = new JCheckBox("Batch Remove Punctuation");
		batchPunctBox.addActionListener(this);
		c.insets = new Insets(3,5,20,10);
		tokPanel.add(batchPunctBox,c);
		
		c.insets = new Insets(3,5,3,10);
		remQuotes = new JCheckBox("Remove Quotes (single and double)");
		remEnclose = new JCheckBox("Remove Parens");
		remCommas = new JCheckBox("Remove Commas");
		remSentSeps = new JCheckBox("Remove Sentence Separators");
		remBreaks = new JCheckBox("Remove Breaks (tabs, carriage returns, newlines)");
		
		c.gridy=2; tokPanel.add(remQuotes,c);
		c.gridy=3; tokPanel.add(remEnclose,c);
		c.gridy=4; tokPanel.add(remCommas,c);
		c.gridy=5; tokPanel.add(remSentSeps,c);
		c.gridy=6; tokPanel.add(remBreaks,c);
		
		return tokPanel;
	}
	
	private JPanel makeFeatureSelectorPanel()
	{
		JPanel fsPanel = new JPanel();
		
		Dimension fieldSize = new Dimension(40,22);
		Dimension minFieldSize = new Dimension(40,22);
		
		fsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Feature Selector"));
		fsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		c.insets = new Insets(3,5,3,10);
		
		c.gridx = 0;
//		c.gridwidth = 2;
//		JLabel fsLab = new JLabel("Feature Selector:");
//		fsLab.setFont(new Font("sanserif",Font.BOLD,14));
//		fsPanel.add(fsLab,c);
		
		c.gridy = 1;
		c.gridx = 1;
		c.gridwidth = 1;
		removeNumsBox = new JCheckBox("Remove Numbers");
		fsPanel.add(removeNumsBox,c);
		
		c.gridx = 0;
		c.gridy=2;
		JLabel stopWordsDirLabel  = new JLabel("Stop Words Dir: ");
		fsPanel.add(stopWordsDirLabel,c);
		
		c.gridx = 1;
		c.weightx = 1;
		stopWordsDirField = new JTextField("");
		stopWordsDirField.setPreferredSize(fieldSize);
		stopWordsDirField.setMinimumSize(minFieldSize);
		fsPanel.add(stopWordsDirField,c);
		
		c.gridx=2;
		c.weightx=0;
		JButton browse = new JButton("Browse");
		browse.setMinimumSize(new Dimension(10,22));
		browse.setPreferredSize(new Dimension(100,22));
		browse.setMaximumSize(new Dimension(130,22));
		browse.setActionCommand("Browse Stopwords Dir");
		browse.addActionListener(this);
		fsPanel.add(browse,c);
		
		JLabel maxPropLabel = new JLabel("Max Docs/Feature (prop):");
		c.gridy=3;
		c.gridx = 0;
		c.weightx = 0;
		fsPanel.add(maxPropLabel,c);
		
		c.gridx = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		maxPropField = new JTextField();		
		maxPropField.setPreferredSize(fieldSize);
		maxPropField.setMinimumSize(minFieldSize);
		fsPanel.add(maxPropField,c);
		
		JLabel minPropLabel = new JLabel("Min Docs/Feature (prop):");
		c.gridy=4;
		c.gridx = 0;
		c.weightx = 0;
		fsPanel.add(minPropLabel,c);
		
		c.gridx = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		minPropField = new JTextField();		
		minPropField.setPreferredSize(fieldSize);
		minPropField.setMinimumSize(minFieldSize);
		fsPanel.add(minPropField,c);
		
		
		return fsPanel;
	}
	
	private JPanel makeBuildPanel()
	{
		Dimension fieldSize = new Dimension(200,22);
		Dimension minFieldSize = new Dimension(70,22);
		
		Dimension smallFieldSize = new Dimension(45,22);
		Dimension minSmallFieldSize = new Dimension(45,22);
		
		JPanel setupPanel =  new JPanel(new GridBagLayout());
		setupPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Setup"));
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3,5,3,10);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		
//		c.weightx = 1;
		c.gridy= 0;
		c.gridwidth=1;
		JLabel mDirLabel = new JLabel("Model dir:");
		setupPanel.add(mDirLabel,c);
		
		c.gridx = 1;
		c.weightx=1;
		modelDirField = new JTextField();
		modelDirField.setPreferredSize(fieldSize);
		modelDirField.setMinimumSize(minFieldSize);
		setupPanel.add(modelDirField,c);
		
		c.gridx=2;
		c.weightx=0;
		JButton browse = new JButton("Browse");
		browse.setMinimumSize(new Dimension(10,22));
		browse.setPreferredSize(new Dimension(100,22));
		browse.setMaximumSize(new Dimension(130,22));
		browse.setActionCommand("Browse Model Dir");
		browse.addActionListener(this);
		setupPanel.add(browse,c);
		
		c.gridx= 0;
		c.gridy= 1;
		JLabel rHomeLab = new JLabel("R Home Dir:");
		setupPanel.add(rHomeLab,c);
		
		c.gridx= 1;
		c.weightx=1;
		rHomeField = new JTextField();
		rHomeField.setPreferredSize(fieldSize);
		rHomeField.setMinimumSize(minFieldSize);
		setupPanel.add(rHomeField,c);
		
		c.gridx=2;
		c.weightx=0;
		JButton rDirBrowse = new JButton("Browse");
		rDirBrowse.setMinimumSize(new Dimension(10,22));
		rDirBrowse.setPreferredSize(new Dimension(100,22));
		rDirBrowse.setMaximumSize(new Dimension(130,22));
		rDirBrowse.setActionCommand("Browse R Home Dir");
		rDirBrowse.addActionListener(this);
		setupPanel.add(rDirBrowse,c);
		
		c.gridx= 0;
		c.gridy= 2;
		JLabel tsLabel = new JLabel("Timestep:");
		setupPanel.add(tsLabel,c);
		
		c.gridx=1;
		tsDay = new JRadioButton("Day");
		tsDay.setSelected(true);
		tsDay.setActionCommand("Other Timestep");
		tsDay.addActionListener(this);
		setupPanel.add(tsDay,c);
		
		c.gridy=3;
		tsHour = new JRadioButton("Hour");
		tsHour.setActionCommand("Other Timestep");
		tsHour.addActionListener(this);
		setupPanel.add(tsHour,c);
		
		c.gridy=4;
		tsMin = new JRadioButton("Minute");
		tsMin.setActionCommand("Other Timestep");
		tsMin.addActionListener(this);
		setupPanel.add(tsMin,c);
		
		c.gridy=5;
		tsMillis = new JRadioButton("Milliseconds");
		tsMillis.setActionCommand("Millis Timestep");
		tsMillis.addActionListener(this);
		setupPanel.add(tsMillis,c);
		
		c.gridx=2;
		tsMillisField = new JTextField();
		tsMillisField.setEnabled(false);
		tsMillisField.setPreferredSize(smallFieldSize);
		tsMillisField.setMinimumSize(minSmallFieldSize);
		setupPanel.add(tsMillisField,c);
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(tsDay);
		bg.add(tsHour);
		bg.add(tsMin);
		bg.add(tsMillis);
		
		JPanel paramsPanel = new JPanel(new GridBagLayout());
		paramsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Sampling Params"));
		
		GridBagConstraints ppC = new GridBagConstraints();
		ppC.insets = new Insets(3,5,3,10);
		JLabel kLab = new JLabel("k(num topics):");
		paramsPanel.add(kLab,ppC);
		
		ppC.gridx=1;
		kField = new JTextField();
		kField.setPreferredSize(smallFieldSize);
		kField.setMinimumSize(minSmallFieldSize);
		paramsPanel.add(kField,ppC);
		
		ppC.gridx=2;
		JLabel nIterLab = new JLabel("n iters:");
		paramsPanel.add(nIterLab,ppC);
		
		ppC.gridx=3;
		nIterField = new JTextField();
		nIterField.setPreferredSize(smallFieldSize);
		nIterField.setMinimumSize(minSmallFieldSize);
		paramsPanel.add(nIterField,ppC);
		
		
		ppC.gridx=0;
		ppC.gridy=1;
		JLabel alphaLab = new JLabel("alpha:");
		paramsPanel.add(alphaLab,ppC);
		
		ppC.gridx=1;
		alphaField = new JTextField();
		alphaField.setPreferredSize(smallFieldSize);
		alphaField.setMinimumSize(minSmallFieldSize);
		paramsPanel.add(alphaField,ppC);
		
		ppC.gridx=2;
		JLabel nBurninLab = new JLabel("n burnin:");
		paramsPanel.add(nBurninLab,ppC);
		
		ppC.gridx=3;
		nBurninField = new JTextField();
		nBurninField.setPreferredSize(smallFieldSize);
		nBurninField.setMinimumSize(minSmallFieldSize);
		paramsPanel.add(nBurninField,ppC);
		
		ppC.gridx=0;
		ppC.gridy=2;
		JLabel etaLab = new JLabel("eta:");
		paramsPanel.add(etaLab,ppC);
		
		ppC.gridx=1;
		etaField = new JTextField();
		etaField.setPreferredSize(smallFieldSize);
		etaField.setMinimumSize(minSmallFieldSize);
		paramsPanel.add(etaField,ppC);
		
		JPanel buildPanel = new JPanel(new GridBagLayout());
		
		c = new GridBagConstraints();
		c.insets = new Insets(5,5,5,5);
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx =1;
		
		buildPanel.add(setupPanel,c);
		
		c.gridy=1;
		buildPanel.add(paramsPanel,c);

		return buildPanel;
	}
	
	public void setPPFinished(boolean b)
	{
		startPP.setEnabled(true);
		resetPP.setEnabled(false);
		startBuild.setEnabled(true);
		if (b)
		{
			
			JOptionPane.showMessageDialog(this, "Preprocessing complete!");
		}else
		{
			addLogOut("Preprocessing interrupted!");
		}
		
		
	}
	
	public void setBuildFinished(boolean b)
	{
		if (b)
		{
			startBuild.setEnabled(true);
			resetBuild.setEnabled(false);
			startPP.setEnabled(true);
			int response = JOptionPane.showConfirmDialog(this, "Model building completed!! Show model in Explore?");
			if (response==JOptionPane.YES_OPTION) 
			{
				tmUI.openModelInExplore();
			}
		}else
		{
			addLogError("Build failed!!");
			JOptionPane.showMessageDialog(this, "Error while building model!! See console for details!!");
		}
	}
	
	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1024,768);
		
		Properties props = tm.utils.IOUtils.loadPropsFile("src/test/resources/test.properties");
		BuildUI bUI = new BuildUI(null);
		bUI.setProperties(props);
		bUI.updateUIFromProps();
		
		frame.getContentPane().add(bUI);
		
		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent ae) 
	{
		if (ae.getActionCommand().equals("Show File Source Panel"))
		{
			CardLayout cardLayout = (CardLayout)sourceCards.getLayout();
			cardLayout.show(sourceCards, FILESOURCEPANEL);
			
		}
		else if (ae.getActionCommand().equals("Show DB Source Panel"))
		{
			CardLayout cardLayout = (CardLayout)sourceCards.getLayout();
			cardLayout.show(sourceCards, DBSOURCEPANEL);
			
		}
		else if (ae.getActionCommand().equals("Millis Timestep"))
		{
			tsMillisField.setEnabled(true);
		}
		else if (ae.getActionCommand().equals("Other Timestep"))
		{
			tsMillisField.setEnabled(false);
		}
		else if (ae.getActionCommand().equals("Batch Remove Punctuation"))
		{
			boolean enable = !batchPunctBox.isSelected();

			remQuotes.setEnabled(enable);
			remBreaks.setEnabled(enable);
			remCommas.setEnabled(enable);
			remEnclose.setEnabled(enable);
			remSentSeps.setEnabled(enable);
			
		}
		else if (ae.getActionCommand().equals("Start Preprocessing"))
		{
			//Update props for UI values
			updatePropsFromUI();
			
			//Validate form
			List<String> errorList = validatePreprocessingConfig();
			
			if (errorList.isEmpty())
			{
				consoleArea.setBorder(standardConsoleBorder);
				
				//Spawn new thread to run preprocessing in background.
				startPP.setEnabled(false);
				resetPP.setEnabled(true);
				startBuild.setEnabled(false);
				
				spt = new StreamingPreprocessorThread(props, this);
				spt.start();
			}
			else
			{
				consoleArea.setBorder(errorConsoleBorder);
				//show error dialog
//				tmUI.getTMDialog().showFormErrorDialog("Preprocessing Errors", errorList);
				consoleArea.setText("");
				for (String error:errorList)
				{
					addLogError(error+"\n");
				}
				
			}
			
		}
		else if (ae.getActionCommand().equals("Stop Preprocessing"))
		{
			spt.interruptPrepocessing();
		}
		else if (ae.getActionCommand().equals("Start Build"))
		{
			
			updatePropsFromUI();
			List<String> errorList = validateBuildConfig();
			
			if (errorList.isEmpty())
			{
				consoleArea.setBorder(standardConsoleBorder);
				
				startBuild.setEnabled(false);
				resetBuild.setEnabled(true);
				startPP.setEnabled(false);
				modelProcessor = new LDAModelProcessor(props);
				modelProcessor.addLoggable(this);
				LDAModelProcessorThread mpt = new LDAModelProcessorThread(this);
				mpt.start();
			}else
			{
				consoleArea.setBorder(errorConsoleBorder);
				//show error dialog
//				tmUI.getTMDialog().showFormErrorDialog("Model Build Errors", errorList);
				consoleArea.setText("");
				for (String error:errorList)
				{
					addLogError(error+"\n");
				}
			}
			
		}
		else if (ae.getActionCommand().equals("Stop Build"))
		{
			modelProcessor.stopBuild();
			startBuild.setEnabled(true);
			resetBuild.setEnabled(false);
			startPP.setEnabled(true);
		}
		
		else if (ae.getActionCommand().startsWith("Browse"))
		{
			String browseType = ae.getActionCommand().replace("Browse ", "");
			File file = IOUtils.showFileLoader(".", "Browse for "+browseType,true);
			
			//If cancel is pressed, file is null. Just return
			if (file== null) return;

			if (browseType.equals("Source Files Dir"))
			{
				fileDirField.setText(file.getAbsolutePath());
				
			}
			else if (browseType.equals("Stopwords Dir"))
			{
				stopWordsDirField.setText(file.getAbsolutePath());
			}
			else if (browseType.equals("Model Dir"))
			{
				modelDirField.setText(file.getAbsolutePath());
			}else if (browseType.equals("R Home Dir"))
			{
				rHomeField.setText(file.getAbsolutePath());
			}
		}
	}
	
	private List<String> validatePreprocessingConfig()
	{
		String source = props.getProperty("TopicModeler.Preprocessor.DocumentSource");
		if (source.trim().toLowerCase().contains("file"))
		{
			return validatePreprocessingConfigFromFile();
		}else
		{
			//TODO: Add validation method for DB
			return new ArrayList<String>();
		}
	}
	
	/**Returns a list of errors in preprocessing validation for files.
	 * 
	 * @return
	 */
	private List<String> validatePreprocessingConfigFromFile()
	{
		List<String> errorList = new ArrayList<String>();
		
		
		//Check that source file exists
		String fileDirString = props.getProperty("TopicModeler.Preprocessor.SourceFile.Directory");
		if (!fileDirString.endsWith("/")) fileDirString += "/";
		File fileDir = new File(fileDirString);
		if (!fileDir.exists())
		{
			errorList.add("Source file directory does not exist");			
		}
	
		//Check for at least one file with source file extension
		if (errorList.isEmpty())
		{
			String ext = props.getProperty("TopicModeler.Preprocessor.SourceFile.Extension");
			File[] files = fileDir.listFiles();
			boolean found = false;
			for (File file:files)
			{
				if (file.getName().endsWith(ext))
				{
					found = true;
					break;
				}
			}
			
			if (!found) errorList.add("Could not find any source files with specified file extension");
		}
		
		//Make sure metadata dir exists
		String metadataDir = "metadata";
		if (errorList.isEmpty())
		{	
			metadataDir = props.getProperty("TopicModeler.Preprocessor.SourceFile.Metadata.Directory");
			
			
			if (!IOUtils.childFileExists(fileDir, metadataDir))
			{
				errorList.add("Metadata directory does not exist in source file directory");
			}
		}
		
		if (!metadataDir.endsWith("/")) metadataDir+="/";
		
		
		//Check that info.csv file exists 
		File infoFile = null;
		if (errorList.isEmpty())
		{
			infoFile = new File(fileDirString+metadataDir+props.getProperty("TopicModeler.Preprocessor.SourceFile.Metadata.InfoFile"));
			if (!infoFile.exists()) {
				errorList.add("info.csv file does not exist within metadata directory!");
			}
		}
		Map<String,String[]> mdMap = null;
		//Check that info.csv file contains at least the time metadata info 
		if (errorList.isEmpty())
		{
			mdMap = IOUtils.loadMetadataInfo(fileDirString+metadataDir+infoFile.getName());
			
			if (!mdMap.containsKey("time")) {
				errorList.add("info.csv file does not contain time entry");
			}
		}
		
		
		//Check that the time metadata file exists
		File timeFile = null;
		if (errorList.isEmpty())
		{
			timeFile = new File(fileDirString+metadataDir+mdMap.get("time")[0]);
			if (!timeFile.exists()) {
				errorList.add("Metadata file for time does not exist within metadata dir: "+timeFile.getName());
			}
		}
		//Check format of dates (independent of source file errors)
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date[] dateRange = new Date[2];
		Date theDate = null;
		//TODO: If dates don't exist, this split return null. Fix.
		String[] datesStr = props.getProperty("TopicModeler.Preprocessor.SourceFile.Timerange").split(",");
		try {
			dateRange[0] = sdf.parse(datesStr[0]);
			dateRange[1] = sdf.parse(datesStr[1]);
		} catch (ParseException e) {
			//If parse problem, try day format
			sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				dateRange[0] = sdf.parse(datesStr[0]);
				dateRange[1] = sdf.parse(datesStr[1]);
			} catch (ParseException e1) {
				errorList.add("Could not parse start and end times. They must be in the format: 'yyyy-MM-dd HH:mm:ss' or 'yyyy-MM-dd'.");
			}
		}
		
		//Check the number of documents within time range
		if (errorList.isEmpty())
		{
			try {
				CSVReader reader = new CSVReader(new FileReader(fileDirString+metadataDir+timeFile.getName()));
			
				String[] line = null;
				int numDocsInRange = 0;
				addLogOut("BuildUI().validationPreprocessingConfigFromFile(): Checking number of docs in time range...");
				while((line = reader.readNext()) !=null)
				{
					try {
						theDate = sdf.parse(line[1]);
						if (theDate.getTime() >= dateRange[0].getTime() && theDate.getTime() <= dateRange[1].getTime())
						{
							numDocsInRange++;
						}
					} catch (ParseException e) {
						//Shouldn't be an error here, but just in case..
						errorList.add("Problem parsing date in time metdata file: "+timeFile.getName());
					}
				}
				if (numDocsInRange == 0)
				{
					errorList.add("There are no documents within specified time range!!");
	
				}
				else if (numDocsInRange <= 100)
				{
					int resp = JOptionPane.showConfirmDialog(this, "There only "+numDocsInRange+" documents in time range. Is this okay?");
					if (resp != JOptionPane.YES_OPTION) 
					{
						errorList.add("Not enough documents within time range");
						
					}
				}
				addLogOut("Number of docs in time range="+numDocsInRange);
	
			} catch (FileNotFoundException e) {
				//Have already checked for file existing
	//								e.printStackTrace();
			} catch (IOException e){}
		}
		
		//Check to see that stopwords dir exists
		String swDirString = props.getProperty("TopicModeler.Preprocessor.FeatureSelector.StopWords.Dir");
		if (!swDirString.endsWith("/")) swDirString += "/";
		File swDir = new File(swDirString);
		if (!swDir.exists())
		{
			errorList.add("Stop words directory does not exist: "+swDirString);
		}
		
		//Check that model directory's parent exists (it will be created in the parent dir in PP if it doesn't
		String modelDir = props.getProperty("TopicModeler.TopicModel.Model.Dir");
		if (!modelDir.endsWith("/")) modelDir += "/";
		File dir = new File(modelDir);
		if (!dir.exists()) {
			addLogOut("BuildUI.validatePreprocessingConfigFromFile(): Model directory doesn't exist. Checking that parent does (new dir will be created)..");
			File parent = dir.getParentFile();
			if (!parent.exists())
			{
				errorList.add("Parent directory of model directory does not exist: "+parent.getAbsolutePath());
			}
		}

		//Check that max and min doc/feature proportions are within 0-1 range

		//First, check that values exist..
		String minPropString = PropertyUtils.getPropertyString("TopicModeler.DocsPerFeature.Minimum");
		if (minPropString == null) errorList.add("Min Docs/Feature has not been specified!");
		
		String maxPropString = PropertyUtils.getPropertyString("TopicModeler.DocsPerFeature.Maximum");
		if (maxPropString == null) errorList.add("Max Docs/Feature has not been specified!");
		
		double minProp = Double.parseDouble(minPropString);
		double maxProp = Double.parseDouble(maxPropString);
		
		if (minProp >= maxProp) errorList.add("Max Docs/Feature Proportion should be more than Min Docs/Feature");
		
		if (minProp < 0 || minProp > 1 || maxProp < 0 || maxProp > 1)
		{
			errorList.add("Min and Max Docs/Feature (proportion) values should be between 0 and 1!");
		}
		
		//Check to see if model dir has been inputted and exists
		if (modelDirField.getText() == "" || !new File(modelDirField.getText()).exists())
		{
			int res = JOptionPane.showConfirmDialog(this, "Model directory has not been specified or is invalid. Would you like to choose a valid one now?");
			if (res==JOptionPane.OK_OPTION)
			{
				File file = IOUtils.showFileLoader(".", "Browse for Model Dir",true);
				modelDirField.setText(file.getAbsolutePath());
				updatePropsFromUI();
			}
			else errorList.add("Valid model directory not selected.");
		}
		
		return errorList;
	}

	private List<String> validateBuildConfig()
	{
		List<String> errorList = new ArrayList<String>();
		
		//Check that model directory exists (it should have been created before/at preprocessing)
		String modelDirString = props.getProperty("TopicModeler.TopicModel.Model.Dir");
		File modelDir = new File(modelDirString);
		if (!modelDirString.endsWith("/")) modelDirString+="/";
		if (!modelDir.exists())
		{
			errorList.add("Model directory does not exist.");
		}
		
		//Check R bin path (both specified dir and PATH env variable)
		String rDir = props.getProperty("TopicModeler.Rhome.BinDir");
		if (!rDir.endsWith("/")) rDir += "/";
		File rDirFile = new File(rDir);
		boolean rScriptFound = false;
		if (rDirFile.exists())
		{
			//In case this is root R dir, look for bin/ subdir
			if (IOUtils.childFileExists(rDirFile, "bin"))
			{
				rDir = rDir+"bin/";
				rDirFile = new File(rDir);
				addLogOut("BuildUI.validateBuildConfig(): Found bin/ directory in R home. Setting home to it.");
				props.setProperty("TopicModeler.Rhome.BinDir", rDir);
			}
			rScriptFound = IOUtils.childFileExists(rDirFile, "Rscript(\\.[^.]*$|$)");		
		}
		//If not found in dir, check PATH
		if (!rScriptFound)
		{
			addLogOut("BuildUI.validateBuildConfig(): Couldn't find Rscript executable in R dir. Trying PATH environment variable");
			
			String[] pathDirs = SystemUtils.getEnvironmentPath();
			for (String pDir:pathDirs)
			{
				File pDirFile = new File(pDir);
				rScriptFound = IOUtils.childFileExists(pDirFile, "Rscript(\\.[^.]*$|$)");
				if (rScriptFound) {
					addLogOut("BuildUI.validateBuildConfig(): Rscript executable found at "+pDir);
					props.setProperty("TopicModeler.Rhome.BinDir", "");
					break;
				}
			}
			//Still not found? Issue error
			if (!rScriptFound)
			{
				 errorList.add("R Home Directory not correct! Must contain Rscript executable (or R /bin folder must be in PATH)");
			}
		}
		
		//TODO: Add validation for timestep (make sure that timestep does not exceed time range and warn if only a few timesteps are contained within the time range)
		
		//Check samplings params for empty fields
		if (PropertyUtils.getPropertyString("TopicModeler.TopicModel.K") == null ||
				PropertyUtils.getPropertyString("TopicModeler.TopicModel.alpha") == null ||
				PropertyUtils.getPropertyString("TopicModeler.TopicModel.eta") == null ||
				PropertyUtils.getPropertyString("TopicModeler.TopicModel.nIter") == null ||
				PropertyUtils.getPropertyString("TopicModeler.TopicModel.nBurnin") == null
				)
		{
			errorList.add("At least one of the sampling parameters are not specified!");
		}
		
		//TODO: Add better validation for sampling params (warn for weird values of all)
		
		
		return errorList;
	}
	
	@Override
	public void addLogOut(String s) 
	{
		try {
			doc.insertString(doc.getLength(), s+"\n", doc.getStyle("font1_black"));
			consoleArea.setCaretPosition(doc.getLength());
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void addLogError(String s) 
	{
		try {
			doc.insertString(doc.getLength(), s+"\n", doc.getStyle("font1_red"));
			consoleArea.setCaretPosition(doc.getLength());
			
			//TODO: Check this. Assumption is that log error meant end of PP or build thread. If so, need to reset buttons.
			startPP.setEnabled(true);
			resetPP.setEnabled(false);
			startBuild.setEnabled(true);
			resetBuild.setEnabled(false);
			
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
}
