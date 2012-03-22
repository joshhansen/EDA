package jhn.wp.articles.visitors.indexing;

import jhn.wp.visitors.SerializingVisitor;
import cc.mallet.types.LabelAlphabet;

public class LabelIndexingVisitor extends SerializingVisitor<LabelAlphabet> {
	public LabelIndexingVisitor(String outputFilename) {
		super(outputFilename);
	}

	public void beforeEverything() {
		index = new LabelAlphabet();
	}
	
	public void visitLabel(String label) {
		index.lookupIndex(label, true);
	}
}