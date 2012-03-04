package jhn.eda;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.apache.lucene.document.Document;
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
		
		r = IndexReader.open(dir);
	
		
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
//			TopicCount tc = null;
//			try {
//				Document d = r.document(termDocs.doc());
//				String label = d.get("label");
//				int topicIdx = topicAlphabet.lookupIndex(label);
//				int count = termDocs.freq();
//				tc = new TopicCount(topicIdx, count);
//			} catch(IOException e) {
//				e.printStackTrace();
//			}
//			return tc;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}

	@Override
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
		
		final String luceneDir = outputDir + "/wp_lucene";
		
		final String targetLabelAlphabetFilename = outputDir + "/dbpedia37_longabstracts_label_alphabet.ser";
		
//		final String datasetFilename = System.getenv("HOME") + "/Projects/topicalguide/datasets/state_of_the_union/imported_data.mallet";
		final String datasetFilename = System.getenv("HOME") + "/Projects/eda_java/toy_dataset2.mallet";
//		final String datasetFilename = System.getenv("HOME") + "/Projects/eda_java/sotu_obama4.mallet";
		
		try {
			System.out.print("Loading label alphabet...");
			LabelAlphabet targetLabelAlphabet = (LabelAlphabet) Util.deserialize(targetLabelAlphabetFilename);
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
