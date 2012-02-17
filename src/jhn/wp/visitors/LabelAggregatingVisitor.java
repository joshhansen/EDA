package jhn.wp.visitors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jhn.eda.Util;
import jhn.wp.exceptions.SkipException;

public class LabelAggregatingVisitor extends Visitor {
	protected int wordsInLabel = 0;
	protected Map<String,Integer> currentLabelWordCounts;
	protected final Set<String> stopwords = Util.stopwords();
	
	@Override
	public void beforeEverything() {
		super.beforeEverything();
		currentLabelWordCounts = new HashMap<String,Integer>();
	}
	
	@Override
	public void visitLabel(String label) throws SkipException {
		super.visitLabel(label);
		wordsInLabel = 0;
	}
	
	@Override
	public void visitWord(String word) {
		super.visitWord(word);
		if(word.isEmpty() || stopwords.contains(word)) return;
		
		Integer count = currentLabelWordCounts.get(word);
		count = count==null? 1 : count+1;
		currentLabelWordCounts.put(word, count);
		wordsInLabel++;
	}

	@Override
	public void afterLabel() throws SkipException {
		currentLabelWordCounts.clear();
		super.afterLabel();
	}
}
