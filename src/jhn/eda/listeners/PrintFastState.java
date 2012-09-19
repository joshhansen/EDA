package jhn.eda.listeners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import jhn.eda.Paths;

/** Print state with one line per document, and only indices */
public class PrintFastState extends IntervalListener {
	private final String runDir;
	private final boolean outputClass;
	
	public PrintFastState(int printInterval, String runDir) {
		this(printInterval, runDir, false);
	}
	
	public PrintFastState(int printInterval, String runDir, boolean outputClass) {
		super(printInterval);
		this.runDir = runDir;
		this.outputClass = outputClass;
		
		File dir = new File(Paths.fastStateDir(runDir));
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}

	@Override
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		String[] docNames = eda.docNames();
		int[] docLengths = eda.docLengths();
		int[][] topics = eda.topics();
		String[] docLabels = outputClass ? eda.docLabels() : null;
		
		try(PrintStream out = new PrintStream(new FileOutputStream(Paths.fastStateFilename(runDir, iteration)))) {
			
			// Header
			out.print("#docnum ");
			if(outputClass) out.print("class ");
			out.println("source token1topic token2topic ... tokenNtopic");
			
			// Body
			
			for (int docNum = 0; docNum < eda.numDocs(); docNum++) {
				out.print(docNum);
				out.print(' ');
				if(outputClass) {
					out.print(eda.allLabels().indexOf(docLabels[docNum], false));
					out.print(' ');
				}
				out.print(docNames[docNum]);
				for (int position = 0; position < docLengths[docNum]; position++) {
					out.print(' ');
					out.print(topics[docNum][position]);
				}
				out.println();
			}
		}
	}
}
