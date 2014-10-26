package jhn.eda.listeners;

import jhn.eda.ProbabilisticExplicitTopicModel;

public class ProbabilisticExplicitTopicModelListener {
	protected ProbabilisticExplicitTopicModel eda;
	
	public void samplerInit(ProbabilisticExplicitTopicModel sampler) {
		this.eda = sampler;
	}
	
	@SuppressWarnings("unused")
	public void iterationStarted(int iteration) throws Exception {
		// Override me
	}
	
	@SuppressWarnings("unused")
	public void iterationEnded(int iteration) throws Exception {
		// Override me
	}
	
	
	public void samplerTerminate() {
		// Override me
	}
}
