package jhn.wp.visitors;

public abstract class AbstractCountingVisitor extends Visitor {

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