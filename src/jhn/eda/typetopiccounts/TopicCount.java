package jhn.eda.typetopiccounts;

import java.io.Serializable;
import java.util.Comparator;

import jhn.util.Util;

public class TopicCount implements Serializable {
	public static final Comparator<TopicCount> cmpTopic = new Comparator<TopicCount>(){
		@Override
		public int compare(TopicCount o1, TopicCount o2) {
			return Util.compareInts(o1.topic, o2.topic);
		}
	};
	
	public static final Comparator<TopicCount> cmpCount = new Comparator<TopicCount>(){
		@Override
		public int compare(TopicCount o1, TopicCount o2) {
			return Util.compareInts(o1.count, o2.count);
		}
	};
	
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
