package jhn.eda.typetopiccounts;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;

import cc.mallet.types.Alphabet;

import jhn.util.Util;
import jhn.wp.Fields;

public class LuceneTypeTopicCounts implements TypeTopicCounts, AutoCloseable {
	private final IndexReader topicWordIdx;
	private final Alphabet typeAlphabet;
	
	public LuceneTypeTopicCounts(IndexReader topicWordIdx, Alphabet typeAlphabet) {
		this.topicWordIdx = topicWordIdx;
		this.typeAlphabet = typeAlphabet;
	}

	private class TermDocsTopicCountIterator implements Iterator<TopicCount> {
		private final TopicCount topicCount = new TopicCount();
		private Term typeTopicTerm = new Term(Fields.text);
		
		private TermDocs termDocs;
		
		public TermDocsTopicCountIterator(String term) throws IOException {
			setTerm(term);
		}

		public void setTerm(String term) throws IOException {
			typeTopicTerm = typeTopicTerm.createTerm(term);
			termDocs = topicWordIdx.termDocs(typeTopicTerm);
		}
		
		@Override
		public boolean hasNext() {
			boolean hasNext = false;
			try {
				hasNext = termDocs.next();
			} catch(IOException e) {
				e.printStackTrace();
			}
			return hasNext;
		}

		@Override
		public TopicCount next() {
			topicCount.topic = termDocs.doc();
			topicCount.count = termDocs.freq();
			
			return topicCount;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public Iterator<TopicCount> typeTopicCounts(int typeIdx) throws TypeTopicCountsException {
		String type = typeAlphabet.lookupObject(typeIdx).toString();
		try {
			return new TermDocsTopicCountIterator(type);
		} catch (IOException e) {
			throw new TypeTopicCountsException(e);
		}
	}

	@Override
	public void close() throws Exception {
		topicWordIdx.close();
		Util.closeIfPossible(typeAlphabet);
	}

}
