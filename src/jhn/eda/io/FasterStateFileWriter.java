package jhn.eda.io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FasterStateFileWriter implements AutoCloseable, StateFileWriter {
	private final boolean includeClass;
	private final DataOutputStream w;
	public FasterStateFileWriter(String outputFilename, boolean includeClass) throws IOException {
		this.includeClass = includeClass;
		this.w = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFilename)));
		w.writeBoolean(includeClass);
	}
	
	/* (non-Javadoc)
	 * @see jhn.eda.io.StateFileWriter#startDocument(int, java.lang.String)
	 */
	@Override
	public void startDocument(int docNum, String docSource) throws IOException {
		if(includeClass) throw new UnsupportedOperationException("Must include class if includeClass==true");
		w.writeInt(docNum);
		w.writeUTF(docSource);
	}
	
	/* (non-Javadoc)
	 * @see jhn.eda.io.StateFileWriter#startDocument(int, java.lang.String, int)
	 */
	@Override
	public void startDocument(int docNum, String docSource, int docClass) throws IOException {
		w.writeInt(docNum);
		w.writeUTF(docSource);
		w.writeInt(docClass);
	}
	
	/* (non-Javadoc)
	 * @see jhn.eda.io.StateFileWriter#nextTokenTopic(int)
	 */
	@Override
	public void nextTokenTopic(int topic) throws IOException {
		w.writeInt(topic);
	}
	
	/* (non-Javadoc)
	 * @see jhn.eda.io.StateFileWriter#endDocument()
	 */
	@Override
	public void endDocument() throws IOException {
		w.writeInt(-1);
	}
	
	@Override
	public void close() throws IOException {
		w.close();
	}
	
}