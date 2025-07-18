package tm.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import tm.topicmodel.ProbabilityValue;
import tm.topicmodel.TopicModelProcessor;

public class FeaturesPanel extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 44128812323138689L;
	
	private ExploreUI ui;
	private TopicModelProcessor modelProcessor;
	private JTable table;

	private ProbabilityTable pTable = new ProbabilityTable();
	private MyTableModel tableModel = new MyTableModel();
	
	private int numFeats = 20;
	
	public FeaturesPanel(TopicModelProcessor modelProcessor)
	{
		this.modelProcessor = modelProcessor;
		
		tableModel = new MyTableModel();
		table = new JTable(tableModel);
		
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		JLabel label = new JLabel("Top Features");
		label.setFont(new Font("serif",Font.BOLD,14));
		label.setHorizontalAlignment(JLabel.CENTER);
		c.insets= new Insets(7,0,0,0);
		add(label,c);
		
		c.gridy = 1;
		c.weighty = 1;
		c.weightx = 1;
		add(new JScrollPane(table),c);
	}
	
	public void updateTopic(int topicId)
	{
		pTable.clear();
		List<ProbabilityValue> pvs = modelProcessor.getTopFeatures(topicId, numFeats);
		for (ProbabilityValue pv:pvs)
		{
			Object[] row = new Object[]{(String)pv.getValue(),pv};
			pTable.addRow(row);
		}
		pTable.sortTable(1, false);
		tableModel.fireTableDataChanged();
		
	}
	
	class MyTableModel extends AbstractTableModel
	{
		String[] columnNames = new String[]{"Feature","Prevalence"};
		/**
		 * 
		 */
		private static final long serialVersionUID = 2L;

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
}
