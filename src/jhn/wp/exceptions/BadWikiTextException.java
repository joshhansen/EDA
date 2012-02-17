package jhn.wp.exceptions;


public abstract class BadWikiTextException extends SkipException {
	public BadWikiTextException(String message, String label) {
		super(message, label);
	}
}