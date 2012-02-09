package jhn.wp.visitors;

import cc.mallet.types.LabelAlphabet;

public class LabelIndexingVisitor extends AbstractIndexingVisitor {
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