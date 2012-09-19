package jhn.eda.typetopiccounts;

import java.io.Serializable;
import java.util.Iterator;

public class ArrayTypeTopicCounts implements TypeTopicCounts, Serializable {
	private static final long serialVersionUID = 1L;
	// Note: array is jagged, so we're not wasting a bunch of space due to sparseness issues
	private final int[][] counts;
	public ArrayTypeTopicCounts(int[][] counts) {
		this.counts = counts;
	}
	
	@Override
	public Iterator<TopicCount> typeTopicCounts(final int typeIdx) throws TypeTopicCountsException {
		return new Iterator<TopicCount>(){
			TopicCount ttc = new TopicCount();
			int idx = 0;
			@Override
			public boolean hasNext() {
				return idx < counts[typeIdx].length - 1; 
			}

			@Override
			public TopicCount next() {
				ttc.topic = counts[typeIdx][idx++];
				ttc.count = counts[typeIdx][idx++];
				return ttc;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
}
