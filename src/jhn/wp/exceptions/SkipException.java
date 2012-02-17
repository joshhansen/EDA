package jhn.wp.exceptions;

public abstract class SkipException extends Exception {
	private static final long serialVersionUID = 1L;

	public SkipException(String message, String label) {
		super("\t[SKIP] " + label + "\t\t\t" + message);
	}
}