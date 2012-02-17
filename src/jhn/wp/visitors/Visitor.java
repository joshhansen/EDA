package jhn.wp.visitors;

import jhn.wp.exceptions.SkipException;

public class Visitor {
	public void beforeEverything() {}
	public void beforeLabel() {}
	public void visitLabel(final String label) throws SkipException {}
	public void visitWord(final String word) {}
	public void afterLabel() throws SkipException {}
	public void afterEverything() {}
}