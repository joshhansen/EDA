package jhn.eda.processor;

class CountingVisitor extends Visitor {
	private int count;
	private final String name;
	
	public CountingVisitor(final String name) {
		this.name = name;
	}
	
	public void beforeEverything() {
		count = 0;
	}
	
	public void afterEverything() {
		System.out.println("Count '" + name + "' = " + count);
	}
	
	protected int increment() {
		return ++count;
	}
	
	protected int count() {
		return count;
	}
}