package jhn.eda.io;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class LibSVMFileWriter implements AutoCloseable {
	private PrintStream out;
	public LibSVMFileWriter(String filename) throws FileNotFoundException {
		out = new PrintStream(new FileOutputStream(filename));
	}
	
	public void startDocument(int classNum) {
		out.print(classNum);
	}
	
	public void featureValue(int featNum, double value) {
		out.print(' ');
		out.print(featNum);
		out.print(value);
	}
	
	public void endDocument() {
		out.println();
	}
	
	@Override
	public void close() {
		out.close();
	}
}