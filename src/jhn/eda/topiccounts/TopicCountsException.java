package jhn.eda.topiccounts;

public class TopicCountsException extends Exception {
	private static final long serialVersionUID = 1L;

	private Exception originalException;
	public TopicCountsException() {
		this(null);
	}
	
	public TopicCountsException(Exception originalException) {
		this.originalException = originalException;
	}
	
	public Exception getOriginalException() {
		return originalException;
	}
}
