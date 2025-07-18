package tm.topicmodel;

import tm.ui.BuildUI;

public class LDAModelProcessorThread extends Thread
{
	private BuildUI bUI;
	public LDAModelProcessorThread(BuildUI bUI)
	{
		
		this.bUI = bUI;
	}
	
	@Override
	public void run()
	{
		TopicModelProcessor tmp = bUI.getTopicModelProcessor();
		int res = tmp.buildModel();
		
		if (res == 0) bUI.setBuildFinished(true);
		else bUI.setBuildFinished(false);
	}
}
