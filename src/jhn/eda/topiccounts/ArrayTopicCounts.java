package jhn.eda.topiccounts;

import java.io.Serializable;

import jhn.util.Factory;

/**
 * NOTE: requires topicID's to be contiguous
 *
 */
public class ArrayTopicCounts implements TopicCounts, Serializable {
	public static Factory<TopicCounts> factory(final int[] counts) {
		return new Factory<TopicCounts>(){
			final TopicCounts inst = new ArrayTopicCounts(counts);
			@Override
			public TopicCounts create() {
				return inst;
			}
		};
	}
	
	private static final long serialVersionUID = 1L;
	
	private final int[] counts;
	
	public ArrayTopicCounts(int[] counts) {
		this.counts = counts;
	}

	@Override
	public int topicCount(int topicID) throws TopicCountsException {
		return counts[topicID];
	}

}
