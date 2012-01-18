package jhn.eda.processor;

public class Visitor {
	public void beforeEverything() {}
	public void beforeLabel() {}
	public void visitLabel(final String label) {}
	public void visitWord(final String word) {}
	public void afterLabel() {}
	public void afterEverything() {}
}