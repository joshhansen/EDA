package jhn.eda.summarize;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class SampleSummaryFileWriter implements AutoCloseable {
	private final boolean includeClass;
	private final PrintStream w;
	
	public SampleSummaryFileWriter(String summaryFilename, boolean includeClass) throws FileNotFoundException {
		this.includeClass = includeClass;
		this.w = new PrintStream(new FileOutputStream(summaryFilename));
		
		w.print("#docnum docsrc ");
		if(includeClass) {
			w.print("class ");
		}
		w.println("topic1id:topic1count topic2id:topic2count ... topicNid:topicNcount");
	}
	
	public void startDocument(int docNum, String docSource) {
		if(includeClass) throw new UnsupportedOperationException("Must include class if includeClass==true");
		_startDocument(docNum, docSource);
	}
	
	public void startDocument(int docNum, String docSource, int docClass) {
		_startDocument(docNum, docSource);
		w.print(docClass);
		w.print(' ');
	}
	
	private void _startDocument(int docNum, String docSource) {
		w.print(docNum);
		w.print(' ');
		w.print(docSource);
	}
	
	public void topicCount(int topic, int count) {
		w.print(' ');
		w.print(topic);
		w.print(':');
		w.print(count);
	}
	
	public void endDocument() {
		w.println();
	}

	@Override
	public void close() {
		w.close();
	}
}
