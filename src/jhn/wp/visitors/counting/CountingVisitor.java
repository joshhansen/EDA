package jhn.wp.visitors.counting;

import jhn.wp.visitors.Visitor;

public abstract class CountingVisitor extends Visitor {

	private int count;
	
	public void beforeEverything() {
		count = 0;
	}

	protected int increment() {
		return ++count;
	}

	protected int count() {
		return count;
	}

}