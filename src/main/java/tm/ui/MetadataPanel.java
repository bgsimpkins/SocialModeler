package tm.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import tm.topicmodel.MetadataVariable;
import tm.topicmodel.ProbabilityValue;
import tm.topicmodel.TopicModelProcessor;

public class MetadataPanel extends JPanel implements ActionListener
{
	private ExploreUI ui;
	private ProbabilityTable pTable = new ProbabilityTable();
	private TopicModelProcessor modelProcessor;
	private MyTableModel tableModel;
	private JTable table;
	private JComboBox combo = new JComboBox();
	
	private int currentTopic = -1;
	
	public MetadataPanel(ExploreUI ui)
	{
		this.ui = ui;
		this.modelProcessor = ui.getTopicModelProcessor();
		
		tableModel = new MyTableModel();
		table = new JTable(tableModel);
		
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		c.gridwidth = 2;
		JLabel label = new JLabel("Top Metadata");
		label.setFont(new Font("serif",Font.BOLD,14));
		label.setHorizontalAlignment(JLabel.CENTER);
		c.insets= new Insets(7,0,0,0);
		add(label,c);
		
		c.gridwidth = 1;
		c.gridy = 1;
		add(combo,c);
		
		c.gridx = 1;
		c.weightx = 0;
		JButton button = new JButton("OK");
		c.fill = GridBagConstraints.NONE;
		button.setPreferredSize(new Dimension(60,30));
		button.setMaximumSize(new Dimension(60,30));
		button.setMinimumSize(new Dimension(60,30));
		button.addActionListener(this);
		add(button,c);
		
		c.gridx = 0;
		c.fill = GridBagConstraints.BOTH;
		c.gridy = 2;
		c.gridwidth = 2;
		c.weighty = 1;
		c.weightx = 1;
		add(new JScrollPane(table),c);
	}
	
	public void updateTopic(int topicId)
	{
		//if (true) return;
		if (currentTopic == -1)			
		{ 			
			this.currentTopic = topicId;
			Map<String,MetadataVariable> mdVars = ui.getTopicModelProcessor().getMetadataVariables();
			combo.removeAllItems();
			Iterator<Entry<String,MetadataVariable>> iter = mdVars.entrySet().iterator();
			while(iter.hasNext())
			{
				Entry<String,MetadataVariable> e = iter.next();
				combo.addItem(e.getValue());
			}
		}
		else
		{
			this.currentTopic = topicId;
			MetadataVariable mdVar = (MetadataVariable)combo.getSelectedItem();
			updateMetadata(mdVar);
		}
		
	}
	
	private void updateMetadata(MetadataVariable mdVar)
	{
		List<ProbabilityValue> mdValList = ui.getTopicModelProcessor().getTopMetaDataValuesForTopic(currentTopic, mdVar, 15);
		
		pTable.clear();
		for (int i = 0; i < mdValList.size(); i++)
		{
			ProbabilityValue mdVal = mdValList.get(i);
			
			pTable.addRow(new Object[]{mdVal.getValue(),mdVal});
		}
		pTable.sortTable(1,false);
		tableModel.fireTableDataChanged();
	}

	class MyTableModel extends AbstractTableModel
	{
		private static final long serialVersionUID = -3678519363529283117L;
		
		String[] columnNames = new String[]{"Value","Prevalence"};

		@Override
		public String getColumnName(int colIndex)
		{
			return columnNames[colIndex];
		}
		@Override
		public int getRowCount() {
			
			return pTable.getNumRows();
		}

		@Override
		public int getColumnCount() {
			
			return 2;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			
//			if (columnIndex==0) return rowIndex;
//			else if (columnIndex==1) return topicNames[rowIndex];
//			else if (columnIndex==2) return topicProbs.get(rowIndex).getProb();
//			else return null;
			return pTable.getCellValue(rowIndex, columnIndex);
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getActionCommand().equals("OK"))
		{
			updateMetadata((MetadataVariable)combo.getSelectedItem());
		}
		
	}
}
