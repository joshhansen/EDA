package jhn.eda.processor;

public class LabelCountingVisitor extends CountingVisitor {
	public LabelCountingVisitor() {
		super("Labels");
	}
	public void visitLabel(String label) {
		int count = increment();
		if(count % 10000 == 0) System.out.println(count + " " + label);
	}
}