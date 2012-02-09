package jhn.wp.visitors;

import jhn.eda.Util;
import cc.mallet.types.Alphabet;

public abstract class AbstractIndexingVisitor extends Visitor {
	protected Alphabet index;
	private final String outputFilename;
	
	public AbstractIndexingVisitor(String outputFilename) {
		this.outputFilename = outputFilename;
	}

	public void afterEverything() {
		Util.serialize(index, outputFilename);
	}
}