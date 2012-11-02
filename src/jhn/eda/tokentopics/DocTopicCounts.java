package jhn.eda.tokentopics;

import it.unimi.dsi.fastutil.ints.IntIterator;

public class DocTopicCounts implements IntIterator {
	private final int docNum;
	private final String docSource;
	private final int[] topics;
	private final int[] docTopicCounts;
	private int pos = -1;
	
	public DocTopicCounts(String line) {
		if(line.startsWith("#")) {
			throw new IllegalArgumentException("Commented lines can't be processed");
		}
		String[] parts = line.split("\\s+");
		
		topics = new int[parts.length - 2];
		docTopicCounts = new int[parts.length - 2];
		
		docNum = Integer.parseInt(parts[0]);
		docSource = parts[1];
		
		String[] subparts;
		for(int i = 2; i < parts.length; i++) {
			subparts = parts[i].split(":");
			topics[i-2] = Integer.parseInt(subparts[0]);
			docTopicCounts[i-2] = Integer.parseInt(subparts[1]);
		}
	}

	@Override
	public boolean hasNext() {
		return pos < topics.length - 1;
	}

	@Override
	public Integer next() {
		return Integer.valueOf(nextInt());
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/** Gives the next topic number */
	@Override
	public int nextInt() {
		return topics[++pos];
	}
	
	public int nextDocTopicCount() {
		return docTopicCounts[pos];
	}
	
	public int docNum() {
		return docNum;
	}
	
	public String docSource() {
		return docSource;
	}

	@Override
	public int skip(int n) {
		throw new UnsupportedOperationException();
	}
}
