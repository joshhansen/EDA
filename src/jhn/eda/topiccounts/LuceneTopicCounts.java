package jhn.eda.topiccounts;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;

import jhn.wp.Fields;

public class LuceneTopicCounts implements TopicCounts, AutoCloseable {
	private final IndexReader topicWordIdx;
	
	public LuceneTopicCounts(IndexReader topicWordIdx) {
		this.topicWordIdx = topicWordIdx;
	}

	@Override
	public int topicCount(int topicID) throws TopicCountsException {
		try {
			final TermFreqVector termFreqVector = topicWordIdx.getTermFreqVector(topicID, Fields.text);
			
			int totalTermFreq = 0;
			if(termFreqVector != null) {
				int[] docTermFreqs = termFreqVector.getTermFrequencies();
				for(int termFreq : docTermFreqs) {
					totalTermFreq += termFreq;
				}
			}
			
			return totalTermFreq;
		} catch (IOException e) {
			throw new TopicCountsException(e);
		}
	}

	@Override
	public void close() throws Exception {
		topicWordIdx.close();
	}

}
