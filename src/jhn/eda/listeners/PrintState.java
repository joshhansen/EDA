package jhn.eda.listeners;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import jhn.eda.Paths;

/** Print state with one line per token */
public class PrintState extends IntervalListener {
	private final String runDir;
	public PrintState(int printInterval, String run) {
		super(printInterval);
		this.runDir = run;
		
		File dir = new File(Paths.stateDir(run));
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}

	@Override
	protected void iterationEndedAtInterval(int iteration) throws FileNotFoundException{
		try(PrintStream out = new PrintStream(new FileOutputStream(Paths.stateFilename(runDir, iteration)))) {
			out.println("#doc source pos typeindex topic");
			for (int docNum = 0; docNum < eda.numDocs(); docNum++) {
				for (int position = 0; position < eda.docLength(docNum); position++) {
					out.print(docNum); out.print(' ');
					out.print(eda.docName(docNum)); out.print(' '); 
					out.print(position); out.print(' ');
					out.print(eda.token(docNum, position)); out.print(' ');
					out.print(eda.topic(docNum, position)); out.println();
				}
			}
		}
	}
}
