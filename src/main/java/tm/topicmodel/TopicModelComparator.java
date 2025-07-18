package tm.topicmodel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tm.utils.IOUtils;

public class TopicModelComparator 
{
	private TopicModelProcessor model1,model2;
	
	public void setModel1(TopicModelProcessor m1)
	{
		this.model1 = m1;
	}
	
	public void setModel2(TopicModelProcessor m2)
	{
		this.model2 = m2;
	}
	
	public ProbabilityDistribution getTopic2CosSimsForModel1(int topicNum, int numFeats)
	{
		List<ProbabilityValue> m1Feats = model1.getTopFeatures(topicNum, numFeats);

		ProbabilityDistribution simDist = new ProbabilityDistribution();
		
		for (int i = 0; i < model2.getNumTopics(); i++)
		{
			//Use map to keep arrays parallel so we can compute cosine sim
			Map<String,ProbabilityValue[]> featMap = new HashMap<String,ProbabilityValue[]>();
			for (ProbabilityValue pv:m1Feats)
			{
				ProbabilityValue zero = new ProbabilityValue();
				zero.setProb(0);
				zero.setValue(pv.getValue());
				featMap.put((String)pv.getValue(),new ProbabilityValue[]{pv,zero});
			}
			
			//System.out.println("Topic "+i+"_______________");
			List<ProbabilityValue> m2Feats = model2.getTopFeatures(i, numFeats);
			
			for (ProbabilityValue pv:m2Feats)
			{
				ProbabilityValue[] pvs = featMap.get((String)pv.getValue());
				if (pvs != null)
				{				
					pvs[1] = pv;
				}else
				{
					ProbabilityValue zero = new ProbabilityValue();
					zero.setProb(0);
					zero.setValue(pv.getValue());
					featMap.put((String)pv.getValue(),new ProbabilityValue[]{zero,pv});
				}
			}
			
			//Strip parallel arrays from map
			
			double[] m1Vec = new double[featMap.size()];
			double[] m2Vec = new double[featMap.size()];
			int j = 0;
			Iterator<Entry<String,ProbabilityValue[]>> iter = featMap.entrySet().iterator();
			while(iter.hasNext())
			{
				Entry<String,ProbabilityValue[]> e = iter.next();
				ProbabilityValue[] pvs = e.getValue();
				m1Vec[j] = pvs[0].getProb();
				m2Vec[j] = pvs[1].getProb();
				j++;
				
				//System.out.println("  "+e.getKey()+": "+pvs[0].getProb()+", "+pvs[1].getProb());
 			}
			double cosSim = cosineSimilarity(m1Vec, m2Vec);
			simDist.addValue(i, cosSim);

		}
		
		return simDist;
	}
	
	public double cosineSimilarity(double[] vec1, double[] vec2)
	{
		//!!Assumption is that vecs are of same length
		
		//Get dot product of vectors
		double dp = 0;
		for(int i = 0; i < vec1.length; i++)
		{
			dp += vec1[i]*vec2[i];
		}
		double denom = Math.sqrt(sumOfSquares(vec1)) * Math.sqrt(sumOfSquares(vec2));
		return dp/denom;
	}
	
	private double sumOfSquares(double[] vec)
	{
		double sum = 0;
		for(int i = 0; i < vec.length; i++)
		{
			sum += Math.pow(vec[i], 2);
		}
		return sum;
	}
	
	public static void main(String[] args)
	{
//		double[] v1 = new double[]{1,2,3,4};
//		double[] v2 = new double[]{1,3,3,5};
//		
//		TopicModelComparator tmc = new TopicModelComparator();
//		
//		System.out.println(""+tmc.cosineSimilarity(v1, v2));
		
		TopicModelProcessor model2 = new LDAModelProcessor(IOUtils.loadPropsFile("src/test/resources/fbTest.properties"));
		TopicModelProcessor model1 = new LDAModelProcessor(IOUtils.loadPropsFile("src/test/resources/fbTest2.properties"));
		
		model2.loadModel();
		model1.loadModel();
		
		TopicModelComparator tmc = new TopicModelComparator();
		tmc.setModel1(model1);
		tmc.setModel2(model2);
		
		ProbabilityDistribution simDist = tmc.getTopic2CosSimsForModel1(5, 100);
		simDist.sortDistDescending();
		List<ProbabilityValue> simList = simDist.getDistributionValues();
		for (ProbabilityValue pv:simList)
		{
			System.out.println(pv.getValue()+": "+pv.getProb());
		}
	}
}
