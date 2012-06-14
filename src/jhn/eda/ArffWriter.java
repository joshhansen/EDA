package jhn.eda;

import java.io.PrintStream;

import weka.core.Attribute;
import weka.core.Instance;

public class ArffWriter {
	private enum State {
		PRE_HEADER,
		HEADER,
		DATA
	}
	private State state = State.PRE_HEADER;
	private PrintStream out;
	
	public ArffWriter(PrintStream out) {
		this.out = out;
	}
	
	public ArffWriter init(String relationName) {
		if(!state.equals(State.PRE_HEADER)) {
			throw new IllegalArgumentException("Init must be called first");
		} else {
			state = State.HEADER;
		}
		out.print("@RELATION ");
		out.println(relationName);
		return this;
	}
	
	public ArffWriter attr(Attribute attr) {
		if(!state.equals(State.HEADER)) {
			throw new IllegalArgumentException("Attributes must be added after init and before data");
		}
		out.println(attr);
		return this;
	}
	
	public ArffWriter inst(Instance inst) {
		switch(state) {
			case HEADER:
				state = State.DATA;
				out.println("@DATA");
				break;
			case DATA:
				break;
			default:
				throw new IllegalArgumentException("Must create header using init(String) and attr(Attribute) prior to writing data");
		}
		
		out.println(inst);
		return this;
	}
	
}
