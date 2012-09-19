package jhn.eda.tokentopics;

import it.unimi.dsi.fastutil.ints.IntIterator;

public class DocTokenTopics implements IntIterator {
	private final int docNum;
	private final String docSource;
	private int tokenIdx;
	private int[] topics;
	public DocTokenTopics(int docNum, String docSource, int[] topics) {
		this.docNum = docNum;
		this.docSource = docSource;
		this.topics = topics;
		tokenIdx = -1;
	}
	
	@Override
	public boolean hasNext() {
		return tokenIdx < topics.length - 1;
	}

	@Override
	public Integer next() {
		return Integer.valueOf(nextInt());
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int nextInt() {
		return topics[++tokenIdx];
	}
	
	public int nextTokenIndex() {
		return tokenIdx + 1;
	}
	
	public int tokenIndex() {
		return tokenIdx;
	}

	@Override
	public int skip(int n) {
		throw new UnsupportedOperationException();
	}
	
	public int docNum() {
		return docNum;
	}
	
	public String docSource() {
		return docSource;
	}
}