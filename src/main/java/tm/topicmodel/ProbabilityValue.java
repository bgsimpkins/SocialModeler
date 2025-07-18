package tm.topicmodel;

import tm.ui.ExploreUI;

public class ProbabilityValue 
{
	private Object value;
	private double prob;
	
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	public double getProb() {
		return prob;
	}
	public void setProb(double prob) {
		this.prob = prob;
	}
	
	public String toString()
	{
		return ExploreUI.PROB_FORMAT.format(prob);
	}
}
