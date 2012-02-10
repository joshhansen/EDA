package jhn.wp.visitors.counting;

public class LabelCountingVisitor extends CountingVisitor {
	public void visitLabel(String label) {
		int count = increment();
		if(count % 10000 == 0) System.out.println(count + " " + label);
	}
	
	public void afterEverything() {
		System.out.println("Label Count = " + count());
	}
}