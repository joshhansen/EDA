package jhn.eda.typetopiccounts;

public class TypeNotInTopic extends TypeTopicCountsException {
	private static final long serialVersionUID = 1L;

	public TypeNotInTopic() {
		super();
	}

	public TypeNotInTopic(Exception originalException) {
		super(originalException);
	}

}
