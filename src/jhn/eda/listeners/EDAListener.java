package jhn.eda.listeners;

import jhn.eda.EDA;

public class EDAListener {
	protected EDA eda;
	public void samplerInit(EDA sampler) {
		this.eda = sampler;
	};
	public void iterationStarted(int iteration) throws Exception {};
	public void iterationEnded(int iteration) throws Exception {};
	public void samplerTerminate() {};
}
