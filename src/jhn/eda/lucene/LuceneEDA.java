package jhn.eda.lucene;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
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
	
	private IndexReader r;
	
	public LuceneEDA(String luceneDir, String logFilename, LabelAlphabet topicAlphabet, double alphaSum, double beta) throws IOException {
		super(logFilename, topicAlphabet, alphaSum, beta);
		
		FSDirectory dir = FSDirectory.open(new File(luceneDir));
		
		this.r = IndexReader.open(dir);
	}
	
	private class TypeTopicCountIterator implements Iterator<TopicCount> {
		private final TermDocs termDocs;

		public TypeTopicCountIterator(String type) throws IOException {
			termDocs = r.termDocs(new Term(Fields.text, type));
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
	
	private static int nextLogNum(String logDir) {
		int max = -1;
		for(File f : new File(logDir).listFiles()) {
			final String fname = f.getName();
			System.out.println(fname);
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
		return logDir + "/" + String.valueOf(nextLogNum(logDir)) + ".txt";
	}
	
	public static void main (String[] args) throws IOException {
		final String outputDir = System.getenv("HOME") + "/Projects/eda/output";
		
		final String logFilename = logFilename(outputDir+"/runs");
		
		final String topicWordIndexName = "wp_lucene3";
		final String luceneDir = outputDir + "/topic_word_idx/" + topicWordIndexName;
		
		final String alphaFilename = outputDir + "/" + topicWordIndexName + "_label_alphabet.ser";
		
		final String datasetName = "debates2012";
		final String datasetFilename = System.getenv("HOME") + "/Projects/eda/datasets/" + datasetName + ".mallet";
		
		try {
			System.out.print("Loading label alphabet...");
			LabelAlphabet targetLabelAlphabet = (LabelAlphabet) Util.deserialize(alphaFilename);
			System.out.println("done.");
			
			InstanceList training = InstanceList.load(new File(datasetFilename));
			
			EDA eda = new LuceneEDA (luceneDir, logFilename, targetLabelAlphabet, 50.0, 0.01);
//			eda.config().put(Options.TYPE_TOPIC_MIN_COUNT, 3);
			eda.config().put(Options.FILTER_DIGITS, false);
			eda.config().put(Options.FILTER_MONTHS, false);
			
			eda.addInstances(training);
			eda.sample(1000);
		} catch(UnknownHostException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}//end main
}
