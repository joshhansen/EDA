package jhn.eda.processor;

import cc.mallet.types.Alphabet;

public class WordIndexingVisitor extends AbstractIndexingVisitor {
	public WordIndexingVisitor(String outputFilename) {
		super(outputFilename);
	}

	public void beforeEverything() {
		index = new Alphabet();
	}
	
	public void visitWord(String word) {
		index.lookupIndex(word, true);
	}
}