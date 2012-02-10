package jhn.wp.visitors;

import jhn.wp.visitors.counting.CountingVisitor;

public class PrintingVisitor extends CountingVisitor {

//	@Override
//	public void beforeEverything() {
//		System.out.println("beforeEverything()");
//	}

//	@Override
//	public void beforeLabel() {
//		System.out.println("beforeLabel()");
//	}

	@Override
	public void visitLabel(String label) {
		increment();
		if(count() % 10000 == 0) System.out.println(count() + " " + label);
	}

//	@Override
//	public void visitWord(String word) {
//		System.out.println("visitWord(" + word + ")");
//	}

//	@Override
//	public void afterLabel() {
//		System.out.println("afterLabel()");
//	}

	@Override
	public void afterEverything() {
//		System.out.println("afterEverything()");
	}

}
