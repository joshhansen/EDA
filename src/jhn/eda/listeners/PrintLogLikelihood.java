package jhn.eda.listeners;

import jhn.util.Log;

public class PrintLogLikelihood extends IntervalListener {
	private final Log out;
	public PrintLogLikelihood(Log out, int printInterval) {
		super(printInterval);
		this.out = out;
	}

	@Override
	protected void iterationEndedAtInterval(int iteration) {
		out.println("<" + iteration + "> Log Likelihood: " + eda.modelLogLikelihood());
	}
}
