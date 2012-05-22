package jhn.eda.typetopiccounts;

import java.io.Serializable;
import java.util.Iterator;

public class ArrayTypeTopicCounts implements TypeTopicCounts, Serializable {
	private static final long serialVersionUID = 1L;
	
	private final int[][] counts;
	public ArrayTypeTopicCounts(int[][] counts) {
		this.counts = counts;
	}
	
	@Override
	public Iterator<TypeTopicCount> typeTopicCounts(final int typeIdx) throws TypeTopicCountsException {
		return new Iterator<TypeTopicCount>(){
			TypeTopicCount ttc = new TypeTopicCount();
			int idx = 0;
			@Override
			public boolean hasNext() {
				return idx < counts[typeIdx].length - 1; 
			}

			@Override
			public TypeTopicCount next() {
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
