package jhn.eda.io;

import java.util.Iterator;

import jhn.eda.tokentopics.DocTokenTopics;

public interface StateFileReader extends Iterable<DocTokenTopics>, Iterator<DocTokenTopics>, AutoCloseable {
	// Nothing here
}
