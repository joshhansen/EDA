package jhn.eda.processor;

import jhn.eda.Util;
import cc.mallet.types.Alphabet;

public class IndexingVisitor extends Visitor {
	protected Alphabet index;
	private final String outputFilename;
	
	public IndexingVisitor(String outputFilename) {
		this.outputFilename = outputFilename;
	}

	public void afterEverything() {
		Util.serialize(index, outputFilename);
	}
}