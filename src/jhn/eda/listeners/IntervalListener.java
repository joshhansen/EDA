package jhn.eda.listeners;


public abstract class IntervalListener extends ProbabilisticExplicitTopicModelListener {
	protected final int printInterval;

	public IntervalListener(int printInterval) {
		this.printInterval = printInterval;
	}

	@Override
	public void iterationStarted(int iteration) throws Exception {
		if(iteration % printInterval == 0) {
			iterationStartedAtInterval(iteration);
		}
	}
	
	@SuppressWarnings("unused")
	protected void iterationStartedAtInterval(int iteration) throws Exception {
		// Override me
	}

	@Override
	public void iterationEnded(int iteration) throws Exception {
		if(iteration % printInterval == 0) {
			iterationEndedAtInterval(iteration);
		}
	}
	
	@SuppressWarnings("unused")
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		// Override me
	}
	
}
