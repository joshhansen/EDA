package jhn.eda.processor;

import java.util.HashSet;
import java.util.Set;

public class WordCountingVisitor extends Visitor {
	private Set<String> words = new HashSet<String>();
	public WordCountingVisitor() {
	}
	public void visitWord(String word) {
		words.add(word);
	}
	
	public void afterEverything() {
		System.out.println("Word Types: " + words.size());
	}
}