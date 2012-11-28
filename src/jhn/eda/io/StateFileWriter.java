package jhn.eda.io;

import java.io.IOException;

public interface StateFileWriter extends AutoCloseable {
	void startDocument(int docNum, String docSource) throws IOException;

	void startDocument(int docNum, String docSource, int docClass) throws IOException;

	void nextTokenTopic(int topic) throws IOException;
	
	void endDocument() throws IOException;
}
