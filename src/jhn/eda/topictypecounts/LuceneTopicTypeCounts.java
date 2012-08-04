package jhn.eda.topictypecounts;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;

import cc.mallet.types.Alphabet;

import jhn.util.Util;
import jhn.wp.Fields;

public class LuceneTopicTypeCounts implements TopicTypeCounts, AutoCloseable {
	private final IndexReader topicWordIdx;
	private final Alphabet typeAlphabet;
	
	public LuceneTopicTypeCounts(IndexReader topicWordIdx, Alphabet typeAlphabet) {
		this.topicWordIdx = topicWordIdx;
		this.typeAlphabet = typeAlphabet;
	}

	@Override
	public Iterator<TopicTypeCount> topicTypeCounts(int topicIdx) throws TopicTypeCountsException {
		try {
			return new TypeCountIterator(topicIdx);
		} catch (IOException e) {
			throw new TopicTypeCountsException(e);
		}
	}

	private class TypeCountIterator implements Iterator<TopicTypeCount> {
		private final TopicTypeCount ttc = new TopicTypeCount();
		private final String[] terms;
		private final int[] counts;
		
		private int i = 0;
		
		public TypeCountIterator(int topicIdx) throws IOException {
			TermFreqVector tfv = topicWordIdx.getTermFreqVector(topicIdx, Fields.text);
			if(tfv != null) {
				terms = tfv.getTerms();
				counts = tfv.getTermFrequencies();
			} else {
				terms = new String[0];
				counts = new int[0];
			}
		}
		
		@Override
		public boolean hasNext() {
			return i < terms.length;
		}

		@Override
		public TopicTypeCount next() {
			ttc.typeIdx = typeAlphabet.lookupIndex(terms[i]);
			ttc.count = counts[i];
			i++;
			return ttc;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}

	@Override
	public void close() throws Exception {
		topicWordIdx.close();
		Util.closeIfPossible(typeAlphabet);
	}
}
