package jhn.eda.typetopiccounts;

import java.io.Serializable;

public class TopicCount implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int topic;
	public int count;
	
	public TopicCount() {
		this(-1, -1);
	}
	
	public TopicCount(int topic, int count) {
		this.topic = topic;
		this.count = count;
	}
}
