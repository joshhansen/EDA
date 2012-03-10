package jhn.eda.lucene;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;

import jhn.eda.EDA;
import jhn.eda.EDA.TopicCount;
import jhn.util.Util;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

public class LuceneEDA extends EDA {
	private static final long serialVersionUID = 1L;
	
	private static final String TEXT_FIELD = "text";
	
	private IndexReader r;
	
	public LuceneEDA(String luceneDir, LabelAlphabet topicAlphabet, double alphaSum, double beta) throws IOException {
		super(topicAlphabet, alphaSum, beta);
		
		FSDirectory dir = FSDirectory.open(new File(luceneDir));
		
		this.r = IndexReader.open(dir);
	}
	
	private class TypeTopicCountIterator implements Iterator<TopicCount> {
		private final TermDocs termDocs;

		public TypeTopicCountIterator(String type) throws IOException {
			termDocs = r.termDocs(new Term(TEXT_FIELD, type));
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
			return new TopicCount(termDocs.doc(), termDocs.freq());
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	protected Iterator<TopicCount> typeTopicCounts(int typeIdx) {
		String type = alphabet.lookupObject(typeIdx).toString();
		Iterator<TopicCount> it = null;
		try {
			it = new TypeTopicCountIterator(type);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return it;
	}
	
	public static void main (String[] args) throws IOException {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
		
		final String name = "wp_lucene3";
		final String luceneDir = outputDir + "/" + name;
		
		final String alphaFilename = outputDir + "/" + name + "_label_alphabet.ser";
		
		final String datasetsDir = System.getenv("HOME") + "/Projects/eda/datasets";
		final String datasetFilename = datasetsDir + "/debates2012.mallet";
//		final String datasetFilename = datasetsDir + "/state_of_the_union.mallet";
//		final String datasetFilename = datasetsDir + "/toy_dataset2.mallet";
//		final String datasetFilename = datasetsDir + "/sotu_obama4.mallet";
		
		try {
			System.out.print("Loading label alphabet...");
			LabelAlphabet targetLabelAlphabet = (LabelAlphabet) Util.deserialize(alphaFilename);
			System.out.println("done.");
			
			InstanceList training = InstanceList.load(new File(datasetFilename));
			
			EDA eda = new LuceneEDA (luceneDir, targetLabelAlphabet, 50.0, 0.01);
			eda.addInstances(training);
			eda.sample(1000);
		} catch(UnknownHostException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
}
