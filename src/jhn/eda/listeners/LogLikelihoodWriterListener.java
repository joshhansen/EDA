package jhn.eda.listeners;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class LogLikelihoodWriterListener extends EDAListener implements AutoCloseable  {
	private boolean firstIter = true;
	private DataOutputStream out;
	public LogLikelihoodWriterListener(String filename) throws FileNotFoundException {
		out = new DataOutputStream(new FileOutputStream(filename));
	}

	@Override
	public void iterationStarted(int iteration) throws Exception {
		if(firstIter) {
			firstIter = false;
			out.writeDouble(eda.logLikelihood());
		}
	}

	@Override
	public void iterationEnded(int iteration) throws Exception {
		out.writeDouble(eda.logLikelihood());
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

}
