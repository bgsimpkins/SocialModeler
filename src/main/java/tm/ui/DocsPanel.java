package tm.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import tm.topicmodel.DocumentContent;
import tm.topicmodel.TopicModelProcessor;

public class DocsPanel extends JPanel
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2741901266109214462L;
	
	private ExploreUI ui;
	private TopicModelProcessor modelProcessor;
	private JTable table;
	private ProbabilityTable pTable = new ProbabilityTable();
	private MyTableModel tableModel = new MyTableModel();
	private int numDocs = 20;
	private int currentTopic = -1;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private JLabel topLabel;
	public DocsPanel(ExploreUI ui)
	{
		this.ui = ui;
		this.modelProcessor = ui.getTopicModelProcessor();
		
		table = new JTable(tableModel);
		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent me) {
				tableClicked(me);
			}
		});
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0;
		topLabel = new JLabel("Top Documents");
		topLabel.setFont(new Font("serif",Font.BOLD,14));
		topLabel.setHorizontalAlignment(JLabel.CENTER);
		c.insets= new Insets(7,0,0,0);
		add(topLabel,c);
		
		c.gridy=1;
		c.weightx= 1;
		c.weighty=1;
		add(new JScrollPane(table),c);
	}
	
	public void updateTopic(int topicId)
	{
		if (topicId == currentTopic) return;
		
		pTable.clear();
		List<DocumentContent> dcs = null;

		dcs = modelProcessor.getTopDocuments(topicId, numDocs);

		for (DocumentContent dc:dcs)
		{
			String contentPreview = "";
			if (dc.getContent().length() >= 100) contentPreview = dc.getContent().substring(0, 100);
			else contentPreview = dc.getContent();
			Object[] row = new Object[]{dc.getDoc().getExternalId(),dateFormat.format(dc.getDoc().getTime()),contentPreview,ExploreUI.PROB_FORMAT.format(dc.getProb())};
			pTable.addRow(row);
		}
		pTable.sortTable(3, false);
		tableModel.fireTableDataChanged();
		
		currentTopic = topicId;
		Date[] dateRange = modelProcessor.getTimeRange();
		topLabel.setText("Top Documents: "+dateFormat.format(dateRange[0])+" to "+dateFormat.format(dateRange[1]));
	}
	
	public void updateTimeRange(Date[] timeRange)
	{
		pTable.clear();
		
		System.out.println("DocsPanel.updateTimeRange(): Updating time range: "+dateFormat.format(timeRange[0])+" to "+dateFormat.format(timeRange[1]));
	
		List<DocumentContent> dcs = modelProcessor.getTopDocumentsForTimeRange(currentTopic, timeRange[0], timeRange[1], numDocs);
		for (DocumentContent dc:dcs)
		{
			String contentPreview = "";
			if (dc.getContent().length() > 100) contentPreview = dc.getContent().substring(0, 100);
			else contentPreview = dc.getContent();
			Object[] row = new Object[]{dc.getDoc().getExternalId(),dateFormat.format(dc.getDoc().getTime()),contentPreview,ExploreUI.PROB_FORMAT.format(dc.getProb())};
			pTable.addRow(row);
		}
		pTable.sortTable(3, false);
		tableModel.fireTableDataChanged();
		
		Date[] dateRange = modelProcessor.getTimeRange();
		topLabel.setText("Top Documents: "+dateFormat.format(timeRange[0])+" to "+dateFormat.format(timeRange[1]));
	}
	
	private void tableClicked(MouseEvent me)
	{
		if (me.getClickCount()==2)
		{
			int rowClicked = table.rowAtPoint(me.getPoint());
			long extDocId = (Long)tableModel.getValueAt(rowClicked, 0);
			ui.showDocContentPanel(extDocId);
		}
		
	}
	
	class MyTableModel extends AbstractTableModel
	{
		String[] columnNames = new String[]{"Id","Date","Text Preview", "Prevalence"};
		/**
		 * 
		 */
		private static final long serialVersionUID = 3L;

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
			
			return 4;
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
