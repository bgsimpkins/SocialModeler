package tm.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import tm.preprocessor.Document;
import tm.topicmodel.LDAModelProcessor;
import tm.topicmodel.TopicModelProcessor;
import tm.utils.IOUtils;
import tm.utils.PropertyUtils;

//TODO: Should eventually be a tabbed panel
public class ExploreUI extends JPanel implements TopicChangeListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 889154462996671178L;
	
	
	private Properties props;
	private DocsPanel docsPanel;
	private DocContentPanel docContentPanel;

	private FeaturesPanel featsPanel;
	private TimeSeriesPanel tsPanel;
	private TopicsPanel topicsPanel;
	private MetadataPanel mdPanel;
	
	private TopicModelProcessor modelProcessor;
	public static String MODEL_DIR;
	
	public static DecimalFormat PROB_FORMAT= new DecimalFormat("#.####");
	
	private String modelName = "";
	
	private TopicModelerUI tmUI;
	
	public ExploreUI(TopicModelerUI tmUI)
	{
		this.tmUI = tmUI;
	}
	
	public void setProperties(Properties props)
	{
		this.props = props;
		PropertyUtils.setProperties(props);
	}
	
	public void init()
	{		
		this.removeAll();
		
		MODEL_DIR = props.getProperty("TopicModeler.TopicModel.Model.Dir");
		if (!MODEL_DIR.endsWith("/")) MODEL_DIR += "/";
		
		//Init topics panel, right panel, and overall splitpane
		topicsPanel = new TopicsPanel(modelProcessor,this);
		topicsPanel.setMinimumSize(new Dimension(220,600));
		topicsPanel.setPreferredSize(new Dimension(300,600));
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridLayout(1,1));
		rightPanel.setMinimumSize(new Dimension(500,600));
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,topicsPanel,rightPanel);
		split.setContinuousLayout(true);
		split.setDividerLocation(.3);
		
		setLayout(new GridLayout(1,1));
		add(split);
		
		tsPanel = new TimeSeriesPanel(this);
		tsPanel.setMinimumSize(new Dimension(500,300));
		JPanel bottomPanel = new JPanel();
		bottomPanel.setMinimumSize(new Dimension(500,300));
		bottomPanel.setLayout(new GridLayout(1,1));
		JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,tsPanel,bottomPanel);
		rightSplit.setContinuousLayout(true);
		rightSplit.setDividerLocation(.5);
		rightPanel.add(rightSplit);
		
		featsPanel = new FeaturesPanel(modelProcessor);
		featsPanel.setMinimumSize(new Dimension(200,300));
		featsPanel.setPreferredSize(new Dimension(400,300));
		
		docsPanel = new DocsPanel(this);
		docsPanel.setMinimumSize(new Dimension(300,300));
		docsPanel.setPreferredSize(new Dimension(400,300));
		
		mdPanel = new MetadataPanel(this);
		mdPanel.setMinimumSize(new Dimension(150,300));
		
		JSplitPane bottomRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,docsPanel,mdPanel);
		bottomRightSplit.setContinuousLayout(true);
		bottomRightSplit.setDividerLocation(.6);
		
		JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,featsPanel,bottomRightSplit);
		bottomSplit.setContinuousLayout(true);
		bottomSplit.setDividerLocation(.5);
		bottomPanel.add(bottomSplit);
	
		docContentPanel = new DocContentPanel(this);
	}
	
	public void showDocContentPanel(long extDocId)
	{
//		Connection sourceConn = IOUtils.initSimpleConnectionForSourceDB(props);
//		
//		String docQuery = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.Query");
//		String docIdField = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.IdField");
//		String whereFieldsAll = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.WhereValues");
//		String[] whereFields = whereFieldsAll.split(",");
//		
//		String adnlClauses = props.getProperty("TopicModeler.Preprocessor.SourceDB.DocContent.AdditionalClauses");
//		docQuery += " WHERE "+docIdField+"=?";
//		for (String field:whereFields)
//		{
//			docQuery += " AND "+field;
//		}
//		docQuery += adnlClauses;
//		
//		System.out.println("ExploreUI.showDocContentPanel(): Doc query: "+docQuery);
//		
//		String text = "";
//		try {
//			PreparedStatement ps = sourceConn.prepareStatement(docQuery);
//			ps.setLong(1, extDocId);
//			ResultSet rs = ps.executeQuery();
//			rs.next();
//			text = rs.getString(3);
//		} catch (SQLException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
//		try {
//			sourceConn.close();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		String text = modelProcessor.getDocumentQueryer().getDocumentContent(extDocId);
		Document doc = modelProcessor.getDocument(extDocId);
		docContentPanel.showDocument(doc, text);
	}
	
	@Override
	public void topicChanged(int topicId, String topicName) 
	{
		getFeatsPanel().updateTopic(topicId);
		getDocsPanel().updateTopic(topicId);
		getTsPanel().updateTopic(topicId,topicName);
		getMetadataPanel().updateTopic(topicId);
		
	}
	
	public String getModelName()
	{
		return modelProcessor.getModelName();
	}
	
	public TopicModelProcessor getTopicModelProcessor()
	{
		return modelProcessor;
	}
	
	public void setTopicModelProcessor(TopicModelProcessor tmp)
	{
		this.modelProcessor = tmp;
	}
	
	public DocsPanel getDocsPanel() {
		return docsPanel;
	}

	public void setDocsPanel(DocsPanel docsPanel) {
		this.docsPanel = docsPanel;
	}

	public FeaturesPanel getFeatsPanel() {
		return featsPanel;
	}
	
	public TMDialog getTMDialog()
	{
		return tmUI.getTMDialog();
	}
	
	public TopicModelerUI getTopicModelerUI()
	{
		return tmUI;
	}

	public void setFeatsPanel(FeaturesPanel featsPanel) {
		this.featsPanel = featsPanel;
	}

	public TimeSeriesPanel getTsPanel() {
		return tsPanel;
	}

	public void setTsPanel(TimeSeriesPanel tsPanel) {
		this.tsPanel = tsPanel;
	}

	public TopicsPanel getTopicsPanel() {
		return topicsPanel;
	}
	
	public MetadataPanel getMetadataPanel()
	{
		return mdPanel;
	}

	public void setTopicsPanel(TopicsPanel topicsPanel) {
		this.topicsPanel = topicsPanel;
	}
	
	public void saveTopics()
	{
		try
		{
			topicsPanel.saveTopics();
			JOptionPane.showMessageDialog(this, "Topics Saved for model: "+modelName);
		}catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Error! Could save topics for model");
		}
		
	}
	
	public static void main(String[] args)
	{
		try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Error setting system look and feel!");
        }
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1024,768);
		
		
		
		Properties props = IOUtils.loadPropsFile("src/test/resources/test.properties");
		LDAModelProcessor ldaProcessor = new LDAModelProcessor(props);
		
		//Load test model
		ldaProcessor.loadModel();
		
		ExploreUI ui = new ExploreUI(null);
		
		ui.setProperties(props);
		ui.setTopicModelProcessor(ldaProcessor);
		
		ui.init();
		
		frame.getContentPane().add(ui);
		frame.setVisible(true);
	}

	
}
