package jhn.eda.listeners;

import jhn.util.Log;

public class PrintLogLikelihood extends IntervalListener implements AutoCloseable {
	private final Log out;
	public PrintLogLikelihood(Log out, int printInterval) {
		super(printInterval);
		this.out = out;
	}

	@Override
	protected void iterationEndedAtInterval(int iteration) {
		out.println("<" + iteration + "> Log Likelihood: " + eda.logLikelihood());
	}

	@Override
	public void close() {
		out.close();
	}
}
