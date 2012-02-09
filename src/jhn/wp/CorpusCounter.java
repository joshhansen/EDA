package jhn.wp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import jhn.eda.Util;
import jhn.wp.visitors.Visitor;

public abstract class CorpusCounter {
	private Set<String> stopwords = Util.stopwords();
	private List<Visitor> visitors = new ArrayList<Visitor>();
	
	public abstract void count();
	
	protected boolean isStopword(String s) {
		return stopwords.contains(s);
	}
	
	protected void beforeEverything() {
		for (Visitor v : visitors)
			v.beforeEverything();
	}

	protected void beforeLabel() {
		for (Visitor v : visitors)
			v.beforeLabel();
	}

	protected void visitLabel(final String label) {
		for (Visitor v : visitors)
			v.visitLabel(label);
	}

	protected void visitWord(final String word) {
		for (Visitor v : visitors)
			v.visitWord(word);
	}

	protected void afterLabel() {
		for (Visitor v : visitors)
			v.afterLabel();
	}

	protected void afterEverything() {
		for (Visitor v : visitors)
			v.afterEverything();
	}

	public void addVisitor(Visitor v) {
		visitors.add(v);
	}

	public List<Visitor> visitors() {
		return Collections.unmodifiableList(visitors);
	}
	
	private static final Pattern tokenSplitRgx = Pattern.compile("[^a-z]");
	protected String[] tokenize(final String s) {
		return tokenSplitRgx.split(s.toLowerCase());
	}
}
