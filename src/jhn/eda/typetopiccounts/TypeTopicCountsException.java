package jhn.eda.typetopiccounts;

public class TypeTopicCountsException extends Exception {
	private static final long serialVersionUID = 1L;

	private Exception originalException;
	public Exception getOriginalException() {
		return originalException;
	}

	public TypeTopicCountsException() {
		this(null);
	}
	
	public TypeTopicCountsException(Exception originalException) {
		this.originalException = originalException;
	}
}
