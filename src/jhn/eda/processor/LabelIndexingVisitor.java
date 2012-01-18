package jhn.eda.processor;

import cc.mallet.types.LabelAlphabet;

class LabelIndexingVisitor extends IndexingVisitor {
	public LabelIndexingVisitor(String outputFilename) {
		super(outputFilename);
	}

	public void beforeEverything() {
		index = new LabelAlphabet();
	}
	
	public void visitLabel(String label) {
		int idx = index.lookupIndex(label, true);
		if(idx % 10000 == 0 && idx > 0)
			System.out.println(idx + " " + label);
	}
}