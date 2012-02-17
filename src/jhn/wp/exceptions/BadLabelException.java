package jhn.wp.exceptions;


public class BadLabelException extends SkipException {
	public BadLabelException(String label) {
		super("Bad label", label);
	}
}