package jhn.eda.typetopiccounts;

import java.io.Serializable;

public class TypeTopicCount implements Serializable {
	public int topic;
	public int count;
	
	public TypeTopicCount() {
		this(-1, -1);
	}
	
	public TypeTopicCount(int topic, int count) {
		this.topic = topic;
		this.count = count;
	}
}
