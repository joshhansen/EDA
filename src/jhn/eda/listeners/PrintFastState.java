package jhn.eda.listeners;

import jhn.eda.Paths;
import jhn.eda.io.FastStateFileWriter;

/** Print state with one line per document, and only indices */
public class PrintFastState extends StateWriterListener {
	public PrintFastState(int printInterval, String runDir) throws NoSuchMethodException, SecurityException {
		this(printInterval, runDir, false);
	}
	
	public PrintFastState(int printInterval, String runDir, boolean outputClass) throws NoSuchMethodException, SecurityException {
		super(FastStateFileWriter.class, printInterval, runDir, outputClass);
	}

	@Override
	protected String filename(int iteration) {
		return Paths.fastStateFilename(runDir, iteration);
	}

	@Override
	protected String outputDir() {
		return Paths.fastStateDir(runDir);
	}
}
