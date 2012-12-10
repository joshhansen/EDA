package jhn.eda.listeners;

import java.io.File;
import java.lang.reflect.Constructor;

import jhn.eda.io.StateFileWriter;

/** Print state in a custom serialized format */
public abstract class StateWriterListener extends IntervalListener {
	protected final Constructor<? extends StateFileWriter> writerCtor;
	protected final String runDir;
	protected final boolean outputClass;
	
	
	public StateWriterListener(Class<? extends StateFileWriter> cls, int printInterval, String runDir) throws NoSuchMethodException, SecurityException {
		this(cls, printInterval, runDir, false);
	}
	
	public StateWriterListener(Class<? extends StateFileWriter> cls, int printInterval, String runDir, boolean outputClass) throws NoSuchMethodException, SecurityException {
		super(printInterval);
		
		this.runDir = runDir;
		this.outputClass = outputClass;
		
		File dir = new File(outputDir());
		if(!dir.exists()) {
			dir.mkdirs();
		}
		
		writerCtor = cls.getConstructor(String.class, Boolean.TYPE);
	}
	
	protected abstract String outputDir();
	
	protected abstract String filename(int iteration);
	
	@Override
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		String[] docNames = eda.docNames();
		int[] docLengths = eda.docLengths();
		int[][] topics = eda.topics();
		
		String docSource;
		try(StateFileWriter out = writerCtor.newInstance(filename(iteration), Boolean.valueOf(outputClass))) {
			for (int docNum = 0; docNum < eda.numDocs(); docNum++) {
				docSource = docNames[docNum];
				if(outputClass) {
					out.startDocument(docNum, docSource, eda.allLabels().indexOf(eda.docLabels()[docNum], false));
				} else {
					out.startDocument(docNum, docSource);
				}
				for (int position = 0; position < docLengths[docNum]; position++) {
					out.nextTokenTopic(topics[docNum][position]);
				}
				out.endDocument();
			}
		}
	}
}
