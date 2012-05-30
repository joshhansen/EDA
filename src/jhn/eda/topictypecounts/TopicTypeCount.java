package jhn.eda.topictypecounts;

import java.io.Serializable;

public class TopicTypeCount implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int typeIdx;
	public int count;
	
	public TopicTypeCount() {
		this(-1, -1);
	}
	
	public TopicTypeCount(int typeIdx, int count) {
		this.typeIdx = typeIdx;
		this.count = count;
	}
}
