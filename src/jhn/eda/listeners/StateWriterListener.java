package jhn.eda.listeners;

import java.io.File;
import java.lang.reflect.Constructor;

import jhn.eda.io.StateFileWriter;
import jhn.idx.Index;

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
		int[] docTokenTopics;
		Index<String> allLabels = eda.allLabels();
		String[] docLabels = eda.docLabels();
		
		String docSource;
		try(StateFileWriter out = writerCtor.newInstance(filename(iteration), Boolean.valueOf(outputClass))) {
			for (int docNum = 0; docNum < eda.numDocs(); docNum++) {
				docSource = docNames[docNum];
				if(outputClass) {
					out.startDocument(docNum, docSource, allLabels.indexOf(docLabels[docNum], false));
				} else {
					out.startDocument(docNum, docSource);
				}
				docTokenTopics = topics[docNum];
				for (int position = 0; position < docLengths[docNum]; position++) {
					out.nextTokenTopic(docTokenTopics[position]);
				}
				out.endDocument();
			}
		}
	}
}
