package jhn.eda.tokentopics;

import it.unimi.dsi.fastutil.ints.IntIterator;

public class DocTopicCounts implements IntIterator {
	private final int docNum;
	private final String docSource;
	private int docClass;
	private final int[] topics;
	private final int[] docTopicCounts;
	private int pos = -1;
	
	public DocTopicCounts(String line) {
		this(line, false);
	}
	
	public DocTopicCounts(String line, boolean includesClass) {
		if(line.startsWith("#")) {
			throw new IllegalArgumentException("Commented lines can't be processed");
		}
		String[] parts = line.split("\\s+");
		
		final int headerSize = includesClass ? 3 : 2;
		topics = new int[parts.length - headerSize];
		docTopicCounts = new int[parts.length - headerSize];
		
		int i = 0;
		docNum = Integer.parseInt(parts[i++]);
		docSource = parts[i++];
		
		if(includesClass) {
			docClass = Integer.parseInt(parts[i++]);
		} else {
			docClass = -1;
		}
		
		String[] subparts;
		for(; i < parts.length; i++) {
			subparts = parts[i].split(":");
			topics[i-headerSize] = Integer.parseInt(subparts[0]);
			docTopicCounts[i-headerSize] = Integer.parseInt(subparts[1]);
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
	
	public int docClass() {
		if(docClass < 0) {
			throw new UnsupportedOperationException("Document class cannot be retrieved when includesClass==false");
		}
		return docClass;
	}

	@Override
	public int skip(int n) {
		throw new UnsupportedOperationException();
	}
}
