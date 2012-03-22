package jhn.wp.articles.visitors.indexing;

import jhn.wp.visitors.SerializingVisitor;
import cc.mallet.types.Alphabet;

public class WordIndexingVisitor extends SerializingVisitor<Alphabet> {
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