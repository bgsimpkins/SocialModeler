package tm.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import tm.topicmodel.ProbabilityValue;
import tm.topicmodel.TopicModelProcessor;
import tm.utils.IOUtils;

public class TopicsPanel extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5427169664693654629L;
	
	private TopicChangeListener topicListener;
	private TopicModelProcessor modelProcessor;
	private JTable table;
//	private List<ProbabilityValue> topicProbs;
	private String[] topicDescs;
	private ProbabilityTable pTable = new ProbabilityTable();
	private MyTableModel tableModel = new MyTableModel();
	
	public TopicsPanel(TopicModelProcessor modelProcessor,TopicChangeListener tcl)
	{
		this.modelProcessor = modelProcessor;
		this.topicListener = tcl;
		
		loadTopicData();
		
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		c.weightx = 1;
		JLabel label = new JLabel("Topics");
		label.setFont(new Font("serif",Font.BOLD,14));
//		label.setBackground(Color.orange);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setOpaque(true);
		
		c.insets= new Insets(7,0,0,0);
//		label.setPreferredSize(new Dimension(100,30));
//		label.setMaximumSize(new Dimension(100,20));
		add(label,c);
		
		
		table = new JTable(tableModel);
		
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

			@Override
			public void valueChanged(ListSelectionEvent e) {
				
				tableSelectionChanged(e);
			}
			
		});
		
		//Sort on header clicks
		table.getTableHeader().addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent me)
			{
				tableHeaderClicked(me);
			}
		});
		
		c.gridy = 1;
		c.weighty = 1;
		c.weightx = 1;
		JScrollPane scroller = new JScrollPane(table);
//		scroller.setPreferredSize(new Dimension(200,600));
//		scroller.setMinimumSize(new Dimension(200,600));
		add(scroller,c);
		
	}
	
	private void tableSelectionChanged(ListSelectionEvent e)
	{
		int topicId = (Integer)tableModel.getValueAt(table.getSelectedRow(),0);
		String topicName = (String) tableModel.getValueAt(table.getSelectedRow(), 1);
		topicListener.topicChanged(topicId, topicName);
	}
	
	private void loadTopicData()
	{
		List<ProbabilityValue>topicProbs = modelProcessor.getTopicProbs().getDistributionValues();
		
		List<String[]> rows = IOUtils.loadCSV(new File(ExploreUI.MODEL_DIR+"topicsNamed.csv"));
//		topicNames = new String[modelProcessor.getNumTopics()];
		topicDescs = new String[modelProcessor.getNumTopics()];
		//Skip header
		for (int i = 1; i < rows.size(); i++)
		{
			String[] row = rows.get(i);
			ProbabilityValue pv = topicProbs.get(i-1);
			Object[] tRow = new Object[]{Integer.parseInt(row[1]),row[2],pv};
			pTable.addRow(tRow);
			topicDescs[i-1] = row[3];
		}
		
		
	}
	
	public void saveTopics()
	{
		StringBuffer outBuff = new StringBuffer();
		outBuff.append("model,topic_id,topic_name,topic_desc\n");
		
		//"model","topic_id","topic_name","topic_desc"
		for (int i = 0; i < pTable.getNumRows(); i++)
		{
			int topicId = (Integer)pTable.getCellValue(i, 0);
			String topicName = (String)pTable.getCellValue(i, 1);
			String topicDesc = topicDescs[i];
			outBuff.append("\""+modelProcessor.getModelName()+"\",")
					.append(topicId).append(",\"")
					.append(topicName).append("\",\"")
					.append(topicDesc).append("\"\n");
		}
		
		IOUtils.writeStringToFile(outBuff.toString(), ExploreUI.MODEL_DIR+"topicsNamed.csv");
		
	}
	
	private void tableHeaderClicked(MouseEvent e)
	{
		int index = table.convertRowIndexToModel(table.columnAtPoint(e.getPoint()));
		if (index >= 0)
		{
			pTable.sortTable(index);
			tableModel.fireTableRowsUpdated(0, pTable.getNumRows());
		}
	}
	
	class MyTableModel extends AbstractTableModel
	{
		private String[] columnNames = new String[]{"Id","Name","Prevalence"};
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public boolean isCellEditable(int rowIndex, int colIndex)
		{
			if (colIndex==1) return true;
			else return false;
		}
		
		@Override
		public void setValueAt(Object val, int rowIndex, int colIndex)
		{
			pTable.updateCellValue(val, rowIndex, colIndex);
		}
		
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
			
			return 3;
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
