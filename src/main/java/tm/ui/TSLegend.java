package tm.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import tm.utils.SimplePalette;


public class TSLegend extends JPanel
{
	private List<String> nameList = new ArrayList<String>();
	private Map<String,Color> colorMap;
	private Color background;
	int yPad = 4;
	private Stroke lineStroke = new BasicStroke(2);
	
	private List<JLabel> labList = new ArrayList<JLabel>();
	private JPanel labPanel;
	public TSLegend(Color background)
	{
		this.background = background;
		init();
	}
	
	private void init()
	{
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		//c.weightx=.2;
		c.weighty=1;
		
		JPanel linePanel = new JPanel()
		{
			public void paintComponent(Graphics g)
			{
				paintLabPanel(g,this.getSize());
			}
		};
		Dimension size= new Dimension(30,30);
		linePanel.setMinimumSize(size);
		linePanel.setPreferredSize(size);
//		linePanel.setMaximumSize(size);
		
		add(linePanel,c);
		
		c.gridx = 1;
		c.weightx=1;
		
		labPanel = new JPanel();
		labPanel.setBackground(background);
		add(labPanel,c);
		
	}
	

	public void paintLabPanel(Graphics g,Dimension pSize)
	{
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(background);
		g2.fillRect(0, 0, pSize.width, pSize.height);
//		
//		int ySpace = getSize().height/nameList.size() - yPad;
//		for(int i = 0; i < nameList.size();i++)
//		{
//			String name = nameList.get(i);
//			int x = 10;
//			int y = i*ySpace + i*yPad + ySpace/2 + yPad/2;http://www.nashvillezoo.org/hours-and-rates
//			g2.setColor(colorMap.get(name));
//			g2.setStroke(lineStroke);
//			
//			g2.drawLine(x,y,x+12,y);
//		}
//		
		for (JLabel lab:labList)
		{
			Color col = colorMap.get(lab.getText());
			g2.setColor(col);
			g2.setStroke(lineStroke);
			int y = lab.getLocation().y+lab.getHeight()/2;
			g2.drawLine(10,y,22,y);
		}
		
	}
	
	public void setData(Map<String,Color> colorMap)
	{
		labPanel.removeAll();
		
		nameList.clear();
		this.colorMap = colorMap;
		Iterator<Entry<String,Color>> iter = colorMap.entrySet().iterator();
		labList.clear();
		
		while(iter.hasNext())
		{
			Entry<String,Color> e = iter.next();
			nameList.add(e.getKey());
		}
		Collections.sort(nameList,String.CASE_INSENSITIVE_ORDER);
		labPanel.setLayout(new GridLayout(nameList.size(),1,5,5));
		//Need to update UI for layout to take effect
		labPanel.updateUI();
		for (String name:nameList)
		{
			JLabel lab = new JLabel(name);
			lab.setToolTipText(name);
			labPanel.add(lab);
			labList.add(lab);
		}
		repaint();
	}
	
	public static void main(String[] args)
	{
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(100,300);
		
		Container cPane = frame.getContentPane();
		cPane.setLayout(new GridLayout(1,1));
		TSLegend tsl = new TSLegend(Color.WHITE);
		cPane.add(tsl);
		
		frame.setVisible(true);
		
		Map<String,Color> cMap = new HashMap<String,Color>();
		Color[] colors = SimplePalette.getColors(70, .7f, .7f, 5);
		

		cMap.put("Georgia",colors[0]);
		cMap.put("Tennessee",colors[1]);
		cMap.put("Florida",colors[2]);
		cMap.put("Ohio",colors[3]);
		cMap.put("Pennsylvania State Stuff",colors[4]);
		
		tsl.setData(cMap);
	}
}
