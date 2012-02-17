package jhn.wp.exceptions;


public class RedirectException extends BadWikiTextException {
	public RedirectException(String label) {
		super("Redirect", label);
	}
}