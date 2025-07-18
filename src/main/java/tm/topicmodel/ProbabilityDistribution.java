package tm.topicmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProbabilityDistribution 
{
	private List<ProbabilityValue> distList = new ArrayList<ProbabilityValue>();
	
	public List<ProbabilityValue> getDistributionValues()
	{
		return distList;
	}
	public void addValue(Object value, double prob)
	{
		ProbabilityValue pv = new ProbabilityValue();
		pv.setValue(value);
		pv.setProb(prob);
		distList.add(pv);
	}
	
	public void addAllValues(Collection<ProbabilityValue> values)
	{
		distList.addAll(values);
	}
	
	public void addProbabilityValue(ProbabilityValue pv)
	{
		distList.add(pv);
	}
	
	public List<ProbabilityValue> sortDistDescending()
	{
		Collections.sort(distList,new DistSorter());
		return distList;
	}
	
	class DistSorter implements Comparator<ProbabilityValue>
	{
		
		@Override
		public int compare(ProbabilityValue o1, ProbabilityValue o2) {
			if (o1.getProb() < o2.getProb()) return 1;
			else if (o1.getProb() > o2.getProb()) return -1;
			else return 0;
		}
		
	}
}
