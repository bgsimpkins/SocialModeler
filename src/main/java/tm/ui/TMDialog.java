package tm.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class TMDialog extends JDialog implements ActionListener
{
	public static final int METADATA_CHECKBOX_PANEL = 1;
	private int mode = 0;
	
	private JButton okayButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	
	private List<Object> selectionList = new ArrayList<Object>();
	
	
	public TMDialog()
	{
		//set default size
		this.setSize(320,400);
		this.setResizable(true);
		this.setVisible(false);
		
		okayButton.setActionCommand("TMDialog OK");
		cancelButton.setActionCommand("TMDialog Cancel");
	}
	
	public List<Object> getSelectionFromCheckBoxes()
	{
		return selectionList;
	}
	
	public void showMetadataCheckPanel(String mdVar, List<String> mdVals, ActionListener listener)
	{
		mode = METADATA_CHECKBOX_PANEL;
		
		this.setSize(280,400);
		
		Container contentPane = getContentPane();
		contentPane.removeAll();
		
		setActionListener(listener);
			
		contentPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets(7,15,7,0);
		c.gridwidth = 2;
		
		
		JLabel titleLab = new JLabel(mdVar+" values to display:");
		contentPane.add(titleLab,c);
		
		JPanel checkPanel = new JPanel();
		checkPanel.setLayout(new GridLayout(mdVals.size(),1));
		
		c.fill = GridBagConstraints.BOTH;
		for(int i = 0; i < mdVals.size(); i++)
		{
			JCheckBox cb = new JCheckBox(mdVals.get(i));
			cb.addActionListener(this);
			checkPanel.add(cb);
			
			for (Object sel:selectionList)
			{
				String selStr = (String)sel;
				if (cb.getText().equals(selStr))
					cb.setSelected(true);
			}
		}
		c.gridy=1;
		c.weighty=1;
		JScrollPane scroller = new JScrollPane(checkPanel);
		//scroller.setMinimumSize(new Dimension(100,150));
		//scroller.setPreferredSize(checkPanel.getPreferredSize());
		contentPane.add(scroller,c);
		
		c.gridwidth = 1;
		c.weightx = 0;
		c.weighty = 0;
		c.gridy = 2;
		
		c.insets = new Insets(10,10,10,10);
		contentPane.add(cancelButton,c);
		
		c.gridx = 1;
		contentPane.add(okayButton,c);
		
		setVisible(true);
	}
	
	public void resetMetadataSelections()
	{
		if (mode != METADATA_CHECKBOX_PANEL)
		{
			System.err.println("TMDialog.resetMetadataSelections(): Tried to reset " +
					"metadata selection whilst not in metadata checkbox mode!");
			return;
		}else if (mode == 0) return;
		
		selectionList.clear();
		
	}
	
	private void setActionListener(ActionListener listener)
	{
		//remove old ones
		ActionListener[] listeners = okayButton.getActionListeners();
		//should be the same ones on both buttons
		for (ActionListener l:listeners)
		{
			okayButton.removeActionListener(l);
			cancelButton.removeActionListener(l);
		}
		okayButton.addActionListener(listener);
		cancelButton.addActionListener(listener);
		
		
	}
	
	/**Shows form validation errors for processing of a 'form'
	 * 
	 */
	public void showFormErrorDialog(String title, List<String> errors)
	{
		Container contentPane = getContentPane();
		contentPane.removeAll();
		
		setTitle(title);
		
		JTextArea ta = new JTextArea();
		ta.setWrapStyleWord(true);
		ta.setLineWrap(true);
		ta.setEditable(false);
		
		
		contentPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		contentPane.add(new JScrollPane(ta),c);
		
		for (String err:errors)
		{
			ta.append(err+"\n\n");
		}
		
		setVisible(true);
	}
	
	public static void main(String[] args)
	{
		TMDialog tmd = new TMDialog();
		tmd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		List<String> mdList = new ArrayList<String>();
		mdList.add("Georgia");
		mdList.add("Ohio");
		mdList.add("New York");
		mdList.add("Stupid Florida");
		mdList.add("Tennessee");
		tmd.showMetadataCheckPanel("State", mdList, null);
				
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getSource() instanceof JCheckBox)
		{
			JCheckBox cb = (JCheckBox) e.getSource();
			if (cb.isSelected())
			{
				selectionList.add(cb.getText());
			}else 
			{
				selectionList.remove(cb.getText());
			}
		}
		
	}
}
