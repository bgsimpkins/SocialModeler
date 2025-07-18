package tm.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import tm.topicmodel.ProbabilityValue;

public class ProbabilityTable 
{
	private ArrayList<Object[]> table = new ArrayList<Object[]>();
	private boolean sortAscending = false;
	
	/**
	 * 
	 * @param probCol Column containing the ProbabilityValue
	 */		

	
	public void addRow(Object[] row)
	{
		table.add(row);	
	}
	
	public int getNumRows()
	{
		return table.size();
	}
	
	public Object getCellValue(int row, int col)
	{
		return table.get(row)[col];
	}
	
	public void updateCellValue(Object val, int row, int col)
	{
		table.get(row)[col] = val;
	}
	
	public void sortTable(int col,boolean ascending)
	{
		this.sortAscending = ascending;
		Collections.sort(table,new Sorter(col));
	}
	
	/**This sort toggles the sort order (asc to desc, etc))
	 * 
	 * @param col
	 */
	public void sortTable(int col)
	{
		this.sortAscending = !this.sortAscending;
		Collections.sort(table,new Sorter(col));
	}
	
	public void clear()
	{
		table.clear();
	}
	
	class Sorter implements Comparator<Object[]>
	{
		private int sortCol;	
		
		public Sorter (int col)
		{
			sortCol = col;
		}
		
		@Override
		public int compare(Object[] a1, Object[] a2) 
		{
			Object o1 = a1[sortCol];
			Object o2 = a2[sortCol];
			
			if (o1 instanceof Integer)
			{
				int i1,i2;
				if (sortAscending)
				{
					i1 = (Integer)o1;
					i2 = (Integer)o2;
				}else{
					i1 = (Integer)o2;
					i2 = (Integer)o1;					
				}
				if (i2 < i1) return 1;
				else if (i2 > i1) return -1;
				else return 0;
			}
			else if (o1 instanceof String) 
			{
				String s1,s2;
				if (sortAscending)
				{
					s1 = (String)o1;
					s2 = (String)o2;
				}else{
					s1 = (String)o2;
					s2 = (String)o1;
				}
				int comp = s2.compareTo(s1);
				if (comp < 0) return 1;
				else if (comp > 0) return -1;
				else return comp;
			}
			else if (o1 instanceof ProbabilityValue)
			{
				ProbabilityValue pv1,pv2;
				
				if (sortAscending)
				{
					pv1 = (ProbabilityValue) o1;
					pv2 = (ProbabilityValue) o2;
				}else{
					pv1 = (ProbabilityValue) o2;
					pv2 = (ProbabilityValue) o1;
				}
				
				if (pv2.getProb() < pv1.getProb()) return 1;
				else if (pv2.getProb() > pv1.getProb()) return -1;
				else return 0;
			}
			else{
				String s1,s2;
				if (sortAscending)
				{
					s1 = o1.toString();
					s2 = o2.toString();
				}else{
					s1 = o2.toString();
					s2 = o1.toString();
				}
				int comp = s2.compareTo(s1);
				if (comp > 0) return 1;
				else if (comp < 0) return -1;
				else return comp;
			}
		}
		
	}
}
