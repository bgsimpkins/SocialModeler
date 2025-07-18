package tm.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

import tm.preprocessor.Document;
import tm.topicmodel.MetadataVariable;

public class DocContentPanel extends JDialog
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7433523264191692077L;
	
	private JTextArea textArea = new JTextArea();
	private ExploreUI exploreUI;
	private JPanel mdPanel = new JPanel();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public DocContentPanel(ExploreUI ui)
	{
		this.exploreUI = ui;
//		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setSize(800,600);
		setVisible(false);
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		JScrollPane textScroller = new JScrollPane(textArea,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		textScroller.setMinimumSize(new Dimension(500,300));
		
		mdPanel.setMinimumSize(new Dimension(200,300));
		mdPanel.setLayout(new GridBagLayout());
		
		
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,textScroller,mdPanel);
		split.setContinuousLayout(true);
		split.setDividerLocation(.85);
		getContentPane().add(split);
	}
	
	public void showDocument(Document doc, String text)
	{
		//TODO: get all md variables. Loop through, get values from Document and populate
		mdPanel.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(5,5,5,5);
		JLabel idLab = new JLabel("Doc Id:");
		mdPanel.add(idLab,c);
		
		c.gridx=1;
		c.fill = GridBagConstraints.HORIZONTAL;
		JLabel id = new JLabel(doc.getExternalId()+"");
		mdPanel.add(id,c);
		
		c.gridx=0;
		c.gridy=1;
		c.fill = GridBagConstraints.NONE;
		c.weightx=.8;
		JLabel timeLab = new JLabel("Time:");
		mdPanel.add(timeLab,c);
		
		c.gridx=1;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		JLabel time = new JLabel(sdf.format(doc.getTime()));
		mdPanel.add(time,c);
		
		Map<String,MetadataVariable> mdVars = exploreUI.getTopicModelProcessor().getMetadataVariables();
		
		Iterator<Entry<String,MetadataVariable>> iter = mdVars.entrySet().iterator();
		
		while(iter.hasNext())
		{
			Entry<String,MetadataVariable> e = iter.next();
			String val = doc.getMetadataVal(e.getValue());
			
			c.fill = GridBagConstraints.NONE;
			c.weightx=.8;
			c.gridx=0;
			c.gridy++;
			JLabel mdVarLab = new JLabel(e.getKey()+":");
			mdPanel.add(mdVarLab,c);
			
			c.weightx = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx=1;
			JLabel mdVal = new JLabel(val);
			mdPanel.add(mdVal,c);
		}
		
		textArea.setText(text);
		textArea.setCaretPosition(0);
		setVisible(true);
	}
}
