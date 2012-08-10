package jhn.eda.topictypecounts;

public class TopicTypeCountsException extends Exception {
	private static final long serialVersionUID = 1L;

	private Exception originalException;
	public TopicTypeCountsException() {
		this(null);
	}
	
	public TopicTypeCountsException(Exception originalException) {
		this.originalException = originalException;
	}

	public Exception getOriginalException() {
		return originalException;
	}
	
}
