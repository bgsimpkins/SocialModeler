package tm.topicmodel;

import tm.preprocessor.Document;

public class DocumentContent 
{
	private Document doc;
	private String content;
	private double prob;
	
	public Document getDoc() {
		return doc;
	}
	public void setDoc(Document doc) {
		this.doc = doc;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public double getProb() {
		return prob;
	}
	public void setProb(double prob) {
		this.prob = prob;
	}
}
