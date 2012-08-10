package jhn.eda.listeners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import jhn.eda.Paths;

/** Print state with one line per document, and only indices */
public class PrintFastState extends IntervalListener {
	private final int run;
	
	public PrintFastState(int printInterval, int run) {
		super(printInterval);
		this.run = run;
		
		File dir = new File(Paths.fastStateDir(run));
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}

	@Override
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		try(PrintStream out = new PrintStream(new FileOutputStream(Paths.fastStateFilename(run, iteration)))) {
			out.println ("#docnum class source token1topic token2topic ... tokenNtopic");
			for (int docNum = 0; docNum < eda.numDocs(); docNum++) {
				out.print(docNum);
				out.print(' ');
				out.print(eda.allLabels().indexOf(eda.docLabel(docNum), false));
				out.print(' ');
				out.print(eda.docName(docNum));
				for (int position = 0; position < eda.docLength(docNum); position++) {
					out.print(' ');
					out.print(eda.topic(docNum, position));
				}
				out.println();
			}
		}
	}
}
