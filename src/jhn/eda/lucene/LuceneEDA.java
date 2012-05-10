package jhn.eda.lucene;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

import jhn.eda.EDA;
import jhn.eda.Options;
import jhn.eda.TopicCount;
import jhn.util.Util;
import jhn.wp.Fields;

public class LuceneEDA extends EDA {
	private static final long serialVersionUID = 1L;
	
	private IndexReader topicWordIdx;
	
	public LuceneEDA(IndexReader topicWordIdx, LabelAlphabet topicAlphabet, String logFilename, double alphaSum, double beta) {
		super(logFilename, topicAlphabet, alphaSum, beta);
		this.topicWordIdx = topicWordIdx;
	}
	
	private static class TermDocsTopicCountIterator implements Iterator<TopicCount> {
		private final TermDocs termDocs;
		private final TopicCount topicCount = new TopicCount(); //Reuse this instance to save on gc overhead

		public TermDocsTopicCountIterator(TermDocs termDocs) {
			this.termDocs = termDocs;
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
	
	protected Iterator<TopicCount> typeTopicCounts(int typeIdx) {
		String type = alphabet.lookupObject(typeIdx).toString();
		
		Iterator<TopicCount> it = null;
		try {
			TermDocs termDocs = topicWordIdx.termDocs(new Term(Fields.text, type));
			it = new TermDocsTopicCountIterator(termDocs);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return it;
	}
	
	private static int nextLogNum(String logDir) {
		int max = -1;
		for(File f : new File(logDir).listFiles()) {
			final String fname = f.getName();
			
			if(fname.endsWith(".txt")) {
				String[] parts = fname.split("\\.");
				
				int value = Integer.parseInt(parts[0]);
				if(value > max) {
					max = value;
				}
			}
		}
		return max + 1;
	}
	
	private static String logFilename(String logDir) {
		String filename = logDir + "/" + String.valueOf(nextLogNum(logDir)) + ".txt";
		System.out.println("Writing to log file: " + filename);
		return filename;
	}
	
	private static final boolean LOAD_SERIALIZED_LABEL_ALPHABET = false;
	public static void main (String[] args) throws IOException, ClassNotFoundException {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
		final String indicesDir = outputDir + "/indices/topic_words";
		final String logFilename = logFilename(outputDir+"/runs");
		
		final String topicWordIndexName = "wp_lucene4"; /* "wp_lucene3" */
		final String luceneDir = indicesDir + "/" + topicWordIndexName;
		
		final String datasetName = "toy_dataset2";/* debates2012 */ /*  */ /* state_of_the_union */
		final String datasetFilename = System.getenv("HOME") + "/Projects/eda/datasets/" + datasetName + ".mallet";
		
		
		
		IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(luceneDir)));

		LabelAlphabet topicAlphabet;
		if(LOAD_SERIALIZED_LABEL_ALPHABET) {
			final String alphaFilename = indicesDir + "/" + topicWordIndexName + "_label_alphabet.ser";
			System.out.print("Loading label alphabet...");
			topicAlphabet = (LabelAlphabet) Util.deserialize(alphaFilename);
			System.out.println("done.");
		} else {
			topicAlphabet = new LuceneLabelAlphabet(topicWordIdx);
		}
		
		EDA eda = new LuceneEDA (topicWordIdx, topicAlphabet, logFilename, 50.0, 0.01);
		eda.config().put(Options.TYPE_TOPIC_MIN_COUNT, 3);
		eda.config().put(Options.FILTER_DIGITS, true);
//		eda.config().put(Options.FILTER_MONTHS, true);
		
		InstanceList training = InstanceList.load(new File(datasetFilename));
		eda.addInstances(training);
		
		eda.sample(1000);
	}//end main
}
