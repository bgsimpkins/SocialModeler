package tm.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import tm.topicmodel.MetadataVariable;
import tm.topicmodel.ProbabilityDistribution;
import tm.topicmodel.ProbabilityValue;
import tm.topicmodel.TopicModelProcessor;
import tm.utils.SimplePalette;

public class TimeSeriesPanel extends JPanel implements MouseListener, MouseMotionListener,
	ActionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ExploreUI ui;
	private TopicModelProcessor modelProcessor;
	private JPanel figurePanel;
	private int currentTopic = -1;
	private Color backgroundColor = new Color(200,200,200);
	
	private List<ProbabilityValue> ts = new ArrayList<ProbabilityValue>();
	private Map<String,ProbabilityDistribution> mdTS = new HashMap<String,ProbabilityDistribution>(); 
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private double[] probRange = new double[2];
	
	private String currentTopicName = "";
	private Font titleFont = new Font("serif",Font.BOLD,18);
	private Font axisFont = new Font("serif",Font.PLAIN,12);
	private DecimalFormat decFormat = new DecimalFormat("#.###");
	
	private Date[] selectedRange = new Date[2];
	
	private boolean antiAliasingOn = true;
	
	private Color selColor = new Color(50,150,254,40);
	private Color selBorderColor = new Color(30,120,224,200);
	
	//0=left handle, 1=right handle, 2=whole select window
	private Rectangle[] selRects = new Rectangle[3];
	
	/**Number of labels on each axis (x,y)*/
	private Dimension numAxisLabels = new Dimension(6,5);

	private static final int NO_SELECT = 0;
	private static final int LEFT_HANDLE_SELECT=1;
	private static final int RIGHT_HANDLE_SELECT=2;
	private static final int WINDOW_SELECT=3;
	
	private int selectUpdateX = -1;
	private int prevUpdateX = -1;
	
	private int selectMode = NO_SELECT;
	
	private JComboBox mdVarBox = new JComboBox();
	
	private JButton mdValButton;
	
	private boolean drawGrid = false;

	private Map<String,Color> lineColors = new HashMap<String,Color>();
	
	private JCheckBox totalBox = new JCheckBox("Show Total");
	
	private TSLegend legend;
	
	public TimeSeriesPanel(ExploreUI ui)
	{
		this.ui = ui;
		this.modelProcessor = ui.getTopicModelProcessor();
		
		selRects[0] = new Rectangle();
		selRects[1] = new Rectangle();
		selRects[2] = new Rectangle();
		selectedRange[0] = new Date();
		selectedRange[1] = new Date();
		
		figurePanel = new JPanel()
		{
			/**
			 * 
			 */
			private static final long serialVersionUID = -8684155721492884707L;

			@Override
			public void paintComponent(Graphics g)
			{
				paintFigure(g,getSize().width,getSize().height);
			}
		};
		
		figurePanel.addMouseListener(this);
		figurePanel.addMouseMotionListener(this);
		figurePanel.setLayout(null);
		
		mdVarBox.setLocation(20, 10);
		mdVarBox.setActionCommand("mdVarBoxSelect");
		figurePanel.add(mdVarBox);
		
		mdValButton = new JButton("Select Values");
		mdValButton.addActionListener(this);	
		mdValButton.setSize(mdValButton.getPreferredSize());
		figurePanel.add(mdValButton);

		totalBox.addActionListener(this);
		totalBox.setSize(totalBox.getPreferredSize());
		totalBox.setBackground(null);
		totalBox.setSelected(true);
		figurePanel.add(totalBox);
		
		//setLayout(new GridLayout(1,1));
		setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.weightx=1;
		c.weighty=1;
		
		c.fill = GridBagConstraints.BOTH;
		add(figurePanel,c);
		
		c.gridx=1;
		c.weightx=.01;
		legend = new TSLegend(backgroundColor);
		Dimension size = new Dimension(80,30);
		legend.setMinimumSize(size);
		legend.setPreferredSize(new Dimension(130,30));
		legend.setMaximumSize(new Dimension(150,30));
		add(legend,c);
		
	}
	
	private void paintFigure(Graphics g,int width, int height)
	{
		if (currentTopic == -1) return;
		
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(backgroundColor);
		g2.fillRect(0,0,width,height);
		
		if (antiAliasingOn)
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		Date[] dateRange = modelProcessor.getTimeRange();
		
		int xOffset = 60;
		int yOffset = 40;
		int topOffset = 50;
		
		Point origin = new Point(xOffset,height-yOffset);	
		double xScale = (dateRange[1].getTime()-dateRange[0].getTime())/((double)width-2*(double)xOffset);
		double yScale = (double)(probRange[1]-probRange[0]) /(height-2*(double)yOffset-topOffset);
							
		g2.setColor(Color.BLUE);
		
		if (ts == null) {
//			System.out.println("ts is null!");
			return;
		}
		//Draw time series
		if (totalBox.isSelected())
		{
			for (int i = 0; i < ts.size(); i++)
			{
				if (i == ts.size()-1) break;
				
				ProbabilityValue pv = ts.get(i);			
				Date time = (Date)pv.getValue();
				int x = (int)Math.round(origin.getX() + (double)(time.getTime()-dateRange[0].getTime())/xScale);
				int y =(int)Math.round(origin.getY() - pv.getProb()/yScale);
				
				ProbabilityValue pv2 = ts.get(i+1);			
				Date time2 = (Date)pv2.getValue();
				int x2 = (int)Math.round(origin.getX() + (double)(time2.getTime()-dateRange[0].getTime())/xScale);
				int y2 =(int)Math.round(origin.getY() - pv2.getProb()/yScale);
				
				g2.drawLine(x, y, x2, y2);
			}
		}
		//Draw metadata time series
		if (!mdTS.isEmpty())
		{
			Iterator<Entry<String,ProbabilityDistribution>> mdIter = mdTS.entrySet().iterator();
			
			while(mdIter.hasNext())
			{
				Entry<String,ProbabilityDistribution> e = mdIter.next();
				g2.setColor(lineColors.get(e.getKey()));
				
				List<ProbabilityValue> mdValTS = e.getValue().getDistributionValues();
				
				for (int i = 0; i < mdValTS.size(); i++)
				{
					
					if (i == ts.size()-1) break;
					
					ProbabilityValue pv = mdValTS.get(i);			
					Date time = (Date)pv.getValue();
					int x = (int)Math.round(origin.getX() + (double)(time.getTime()-dateRange[0].getTime())/xScale);
					int y =(int)Math.round(origin.getY() - pv.getProb()/yScale);
					
					ProbabilityValue pv2 = mdValTS.get(i+1);			
					Date time2 = (Date)pv2.getValue();
					int x2 = (int)Math.round(origin.getX() + (double)(time2.getTime()-dateRange[0].getTime())/xScale);
					int y2 =(int)Math.round(origin.getY() - pv2.getProb()/yScale);
					
					g2.drawLine(x, y, x2, y2);
				}
			}
		}
		
		//Draw topic title at top

		g2.setFont(titleFont);
		FontMetrics fm = g2.getFontMetrics();
		int sWidth = SwingUtilities.computeStringWidth(fm, currentTopicName);
		int titleX = width/2-sWidth/2;
		g2.setColor(Color.BLACK);
		g2.drawString(currentTopicName, titleX, 20+topOffset);
		
		//Draw axes
		g2.setFont(axisFont);
		
		g2.drawLine(origin.x, origin.y, width-xOffset, origin.y);
		g2.drawLine(origin.x, origin.y, origin.x, yOffset+topOffset);
		
		Date xAxisDate = new Date();
		fm = g2.getFontMetrics();
		
		////draw axis labels
		
		//x-axis: dates
		for (int i = 0; i <= numAxisLabels.width; i++)
		{
			int x = origin.x + (int)(((double)i/numAxisLabels.getWidth())*(width-2*xOffset));
//			long xMillis = dateRange[0].getTime() + (long)(((double)i/numAxisLabels.getWidth())*(dateRange[1].getTime()-dateRange[0].getTime()));
			long xMillis = dateRange[0].getTime() + (long)(xScale*(x-origin.x));    //need to subtract origin back out to get correct x here
			xAxisDate.setTime(xMillis);
			String label = dateFormat.format(xAxisDate);
			int labWidth = SwingUtilities.computeStringWidth(fm, label);
			int y = height-yOffset/3;
			
			g2.drawString(label, x-labWidth/2, y);
			if (i != 0) g2.drawLine(x, origin.y, x, origin.y+10);
		}
		
		//y-axis: Prevalence
		for (int i = 0; i <= numAxisLabels.height; i++)
		{
			if (i==0) continue;
			
			int y = origin.y - (int)(((double)i/numAxisLabels.getHeight())*(height-2*yOffset-topOffset));	
//			double prev = probRange[0] + yScale*(origin.getY()+(double)y);
		
			double prev = probRange[0] + ((double)i/numAxisLabels.getHeight())*(probRange[1]-probRange[0]);
			String label = decFormat.format(prev);
			int labWidth = SwingUtilities.computeStringWidth(fm, label);
			int labHeight = fm.getAscent();
			
			g2.drawString(label, 3*xOffset/4-labWidth, y+labHeight/2);
			g2.drawLine(origin.x, y, origin.x-10, y);
		}
		
		int x = origin.x + (int)((selectedRange[0].getTime()-dateRange[0].getTime())/xScale);
		int x2 = origin.x + (int)((selectedRange[1].getTime()-dateRange[0].getTime())/xScale);
		
		int handleSize = 17;
		
		//if there is an update, update select range
		if (selectUpdateX != -1)
		{
			if (selectMode == LEFT_HANDLE_SELECT)
			{
				if (selectUpdateX >= origin.x && selectUpdateX < x2-handleSize*2)
				{
					selectedRange[0].setTime((long)(dateRange[0].getTime() + (selectUpdateX-origin.getX())*xScale));
//					x = origin.x + (int)((selectedRange[0].getTime()-dateRange[0].getTime())/xScale);
				}
			}
			else if (selectMode == RIGHT_HANDLE_SELECT)
			{
				if (selectUpdateX <= width-xOffset && selectUpdateX > x+handleSize*2)
				selectedRange[1].setTime((long)(dateRange[0].getTime() + (selectUpdateX-origin.getX())*xScale));
//				x2 = origin.x + (int)((selectedRange[1].getTime()-dateRange[0].getTime())/xScale);
			}
			
			else if (selectMode == WINDOW_SELECT && prevUpdateX != -1)
			{
				//If diff is positive, moving window to right
				int diff = selectUpdateX - prevUpdateX;		
						
				if ((diff > 0 && (x2+diff <= width-xOffset)) ||  //Make sure we don't move window too far to the right
						(diff < 0 && (x+diff >= origin.x)))		//..or left
				{
					long diffTime = (long)(diff*xScale);
//					System.out.println("Difftime="+diffTime);
					selectedRange[0].setTime(selectedRange[0].getTime()+diffTime);
					selectedRange[1].setTime(selectedRange[1].getTime()+diffTime);
				}
//				prevUpdateX = -1;
				
			}
			
			//Update window xs
			x = origin.x + (int)((selectedRange[0].getTime()-dateRange[0].getTime())/xScale);
			x2 = origin.x + (int)((selectedRange[1].getTime()-dateRange[0].getTime())/xScale);
			
			//Update processed. Reset.
			prevUpdateX = selectUpdateX;
			selectUpdateX = -1;
		}
		
		
		//Draw selection
		g2.setColor(selColor);
		
		int selWidth = (int)((selectedRange[1].getTime() - selectedRange[0].getTime())/xScale);
		g2.fillRect(x,yOffset+topOffset,selWidth,height-2*yOffset-topOffset);
		
		g2.setColor(selBorderColor);
		g2.drawRect(x,yOffset+topOffset,selWidth,height-2*yOffset-topOffset);	
		
		//Draw drag triangles		
		Polygon p = new Polygon();
		p.addPoint(x, origin.y);
		p.addPoint(x, origin.y-handleSize);
		p.addPoint(x+handleSize, origin.y);
		g2.fill(p);
		
		p.reset();
		p.addPoint(x2, origin.y);
		p.addPoint(x2, origin.y-handleSize);
		p.addPoint(x2-handleSize, origin.y);
		g2.fill(p);
	
		//Update select click regions
		selRects[0].setBounds(x, origin.y-handleSize, handleSize, handleSize);
		selRects[1].setBounds(x2-handleSize, origin.y-handleSize, handleSize, handleSize);
		selRects[2].setBounds(x,yOffset+topOffset,x2-x,height-2*yOffset-topOffset);
		
		if (drawGrid)
		{
			g2.setColor(Color.WHITE);
			
			for (x = 0; x < getSize().width; x+=100)
			{

				if (x!=0)
				{
					g2.drawLine(x,0,x,getSize().height);
					g2.drawString(""+x,x,20);
				}
				
			}
			
			for (int y = 0; y < getSize().height; y+=100)
			{
				if (y!=0)
				{
					g2.drawLine(0,y,getSize().width,y);
					g2.drawString(""+y,10,y);
				}
			}
		}

	}
	
	public void updateTopic(int topicId, String topicName)
	{
		if (topicId == currentTopic ) return;
		mdTS.clear();
		
		lineColors.clear();
		Map<String,Color> cMap = new HashMap<String,Color>();
		if (totalBox.isSelected()) cMap.put("Total", Color.BLUE);
		legend.setData(cMap);
		ui.getTMDialog().resetMetadataSelections();
		
		Date[] timeRange =  modelProcessor.getTimeRange();
		ts = modelProcessor.getTimeSeriesForTopic(topicId,timeRange[0], timeRange[1]).getDistributionValues();
		updateProbRange(true);

		Map<String,MetadataVariable> mdMap = modelProcessor.getMetadataVariables();
		mdVarBox.removeAllItems();
		Iterator<Entry<String,MetadataVariable>> iter = mdMap.entrySet().iterator();
		while(iter.hasNext())
		{
			Entry<String,MetadataVariable> e = iter.next();
			mdVarBox.addItem(e.getKey());
		}
		mdVarBox.setSize(mdVarBox.getPreferredSize());
		mdValButton.setLocation(mdVarBox.getLocation().x+mdVarBox.getSize().width+20,10);
		totalBox.setLocation(mdValButton.getLocation().x+mdValButton.getSize().width+20,10);
		
		//Get min and max of probs
//		probRange[0] = 1;
//		probRange[1] = 0;
//		
//		for (ProbabilityValue pv:ts)
//		{
//			if (pv.getProb() < probRange[0]) probRange[0] = pv.getProb();
//			else if (pv.getProb() > probRange[1]) probRange[1] = pv.getProb();
//		}
		
		currentTopic = topicId;
		currentTopicName = topicName;
		
		figurePanel.repaint();
		
		Date[] modelRange = modelProcessor.getTimeRange();
		selectedRange[0].setTime(modelRange[0].getTime());
		selectedRange[1].setTime(modelRange[1].getTime());
	}

	private void updateProbRange(boolean useTotal)
	{
		//TODO: Should zero always be the minimum (seems like it should)
		probRange[0] = 0;
		probRange[1] = 0;
		if (useTotal)
		{
			for(ProbabilityValue pv:ts)
			{
				if (pv.getProb() > probRange[1]) probRange[1] = pv.getProb();
			}
		}
		else //just use metadata
		{
			Iterator<Entry<String,ProbabilityDistribution>> iter = mdTS.entrySet().iterator();
			
			while(iter.hasNext())
			{
				ProbabilityDistribution pd = iter.next().getValue();
				
				List<ProbabilityValue> pvs = pd.getDistributionValues();
				for(ProbabilityValue pv:pvs)
				{
					if (pv.getProb() > probRange[1]) probRange[1] = pv.getProb();
				}
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) 
	{
		//If select handles clicked on, set select mode appropriately
		if (selRects[0].contains(e.getPoint())) selectMode = LEFT_HANDLE_SELECT;		
		else if (selRects[1].contains(e.getPoint())) selectMode = RIGHT_HANDLE_SELECT;
		else if (selRects[2].contains(e.getPoint())) selectMode = WINDOW_SELECT;

	}

	@Override
	public void mouseReleased(MouseEvent e) 
	{
		
		//If select mode was previously on, reset and update top docs
		if (selectMode > NO_SELECT)
		{		
			selectMode = NO_SELECT;
			selectUpdateX = -1;
			prevUpdateX = -1;
			
			//Update top documents
			ui.getDocsPanel().updateTimeRange(selectedRange);
		}
	
		
	}

	@Override
	public void mouseDragged(MouseEvent e) 
	{
		if (selectMode==LEFT_HANDLE_SELECT || selectMode==RIGHT_HANDLE_SELECT)
		{
			selectUpdateX = e.getX();			
			figurePanel.repaint();
		}else if (selectMode==WINDOW_SELECT)
		{
//			if (selectUpdateX != -1) prevUpdateX = selectUpdateX;
			selectUpdateX = e.getX();
			
			figurePanel.repaint();
			
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) 
	{
//		prevUpdateX = -1;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {}
	
	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getActionCommand().equals("Select Values"))
		{
			
			String mdVarName = (String)mdVarBox.getSelectedItem();
			MetadataVariable mdVar = ui.getTopicModelProcessor().getMetadataVariables().get(mdVarName);
			ui.getTMDialog().showMetadataCheckPanel(mdVarName, mdVar.getValues(), this);
			
		}
		else if (e.getActionCommand().equals("TMDialog OK"))
		{
			mdTS.clear();
			
			
			//Load metadata for values
			List<Object> mdObjs = ui.getTMDialog().getSelectionFromCheckBoxes();
		    List<String> mdVals = new ArrayList<String>();
		    
		    lineColors.clear();
		    Color[] colors = SimplePalette.getColors(70, .8f, .7f, mdObjs.size());
		    
		    
		    for (int i =0; i < mdObjs.size(); i++)
		    {
		    	Object o = mdObjs.get(i);
		    	String s = (String)o;
		    	mdVals.add(s);
		    	lineColors.put(s, colors[i]);
		    	
		    }
			
			String mdVar = (String)mdVarBox.getSelectedItem();
			Date[] timeRange =  modelProcessor.getTimeRange();
			
			mdTS = modelProcessor.getTimeSeriesForTopicAndMetadata(currentTopic, timeRange[0], 
					timeRange[1], mdVar, mdVals);
			
			updateProbRange(totalBox.isSelected());
			
			ui.getTMDialog().setVisible(false);
			
			Map<String,Color> cMap = new HashMap<String,Color>();
			cMap.putAll(lineColors);
			if (totalBox.isSelected()) cMap.put("Total", Color.BLUE);
			legend.setData(cMap);
			repaint();
			
		}
		else if (e.getActionCommand().equals("TMDialog Cancel"))
		{
			ui.getTMDialog().setVisible(false);
		}
		else if(e.getActionCommand().equals("Show Total"))
		{
			updateProbRange(totalBox.isSelected());
			Map<String,Color> cMap = new HashMap<String,Color>();
			cMap.putAll(lineColors);
			if (totalBox.isSelected()) cMap.put("Total", Color.BLUE);
			legend.setData(cMap);
			repaint();
			
		}
		
	}

}
