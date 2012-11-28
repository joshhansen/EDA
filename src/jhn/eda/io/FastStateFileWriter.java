package jhn.eda.io;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class FastStateFileWriter implements StateFileWriter {
	private final boolean includeClass;
	private final PrintStream w;
	public FastStateFileWriter(String outputFilename, boolean includeClass) throws FileNotFoundException {
		this.includeClass = includeClass;
		this.w = new PrintStream(new FileOutputStream(outputFilename));
		
		// Header
		w.print("#docnum ");
		if(includeClass) w.print("class ");
		w.println("source token1topic token2topic ... tokenNtopic");
	}
	
	
	@Override
	public void startDocument(int docNum, String docSource) {
		if(includeClass) throw new UnsupportedOperationException("Must include class if includeClass==true");
		w.print(docNum);
		w.print(' ');
		w.print(docSource);
	}
	
	@Override
	public void startDocument(int docNum, String docSource, int docClass) {
		w.print(docNum);
		w.print(' ');
		w.print(docClass);
		w.print(' ');
		w.print(docSource);
	}
	
	@Override
	public void nextTokenTopic(int topic) {
		w.print(' ');
		w.print(topic);
	}
	
	@Override
	public void endDocument() {
		w.println();
	}
	
	@Override
	public void close() {
		w.close();
	}
	
}