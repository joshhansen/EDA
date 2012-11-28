package jhn.eda.listeners;

import jhn.eda.Paths;
import jhn.eda.io.FasterStateFileWriter;

/** Print state with one line per document, and only indices */
public class PrintFasterState extends StateWriterListener {
	public PrintFasterState(int printInterval, String runDir) throws NoSuchMethodException, SecurityException {
		this(printInterval, runDir, false);
	}
	
	public PrintFasterState(int printInterval, String runDir, boolean outputClass) throws NoSuchMethodException, SecurityException {
		super(FasterStateFileWriter.class, printInterval, runDir, outputClass);
	}

	@Override
	protected String filename(int iteration) {
		return Paths.fasterStateFilename(runDir, iteration);
	}

	@Override
	protected String outputDir() {
		return Paths.fasterStateDir(runDir);
	}
}
