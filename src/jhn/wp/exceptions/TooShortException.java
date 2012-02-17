package jhn.wp.exceptions;


public class TooShortException extends SkipException {
	private final int length;
	public TooShortException(String label, int length) {
		super("Too short (length " + length + ")", label);
		this.length = length;
	}
	
	public int length() { return length; }
}