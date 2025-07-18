package tm.preprocessor;

import java.util.Properties;

import tm.ui.BuildUI;
import tm.ui.Loggable;

public class StreamingPreprocessorThread extends Thread
{
	private Properties props;
	private BuildUI bUI;
	private StreamingPreprocessor preprocessor;
	public StreamingPreprocessorThread(Properties props, BuildUI buildUI)
	{
		this.props = props;
		this.bUI = buildUI;
		
	}
	
	
	public void run()
	{
		preprocessor = new StreamingPreprocessor(props);
		preprocessor.addLoggable(bUI);
		boolean completed = preprocessor.processDocs();
		bUI.setPPFinished(completed);
		
	}
	
	public void interruptPrepocessing()
	{
		preprocessor.stopProcessing();
	}
}
