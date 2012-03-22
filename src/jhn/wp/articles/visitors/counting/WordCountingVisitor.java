package jhn.wp.articles.visitors.counting;

import java.util.HashSet;
import java.util.Set;

import jhn.wp.visitors.Visitor;

public class WordCountingVisitor extends Visitor {
	private Set<String> words = new HashSet<String>();
	
	public void visitWord(String word) {
		words.add(word);
	}
	
	public void afterEverything() {
		System.out.println("Word Types: " + words.size());
	}
}