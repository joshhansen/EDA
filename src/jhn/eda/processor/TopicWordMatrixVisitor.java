package jhn.eda.processor;

import jhn.eda.Util;
import cc.mallet.types.Alphabet;
import cc.mallet.types.LabelAlphabet;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

public class TopicWordMatrixVisitor extends Visitor {
	private final Alphabet alphabet;
	private final LabelAlphabet labelAlphabet;
	private final String outputFilename;
	
	private DoubleMatrix2D matrix;
	private int currentLabelIdx;

	public TopicWordMatrixVisitor(final LabelAlphabet labelAlphabet, final Alphabet alphabet, final String outputFilename) {
		this.labelAlphabet = labelAlphabet;
		this.alphabet = alphabet;
		this.outputFilename = outputFilename;
	}
	
	@Override
	public void beforeEverything() {
		matrix = new SparseDoubleMatrix2D(labelAlphabet.size(), alphabet.size()); 
	}

	@Override
	public void visitLabel(String label) {
		currentLabelIdx = labelAlphabet.lookupIndex(label);
	}

	@Override
	public void visitWord(String word) {
		final int wordIdx = alphabet.lookupIndex(word);
		final double original = matrix.get(currentLabelIdx, wordIdx);
		matrix.setQuick(currentLabelIdx, wordIdx, original+1.0);//setQuick is OK because we are using same coords as the bounds-checking 'get' method above
	}

	@Override
	public void afterEverything() {
		Util.serialize(matrix, outputFilename);
	}
}
