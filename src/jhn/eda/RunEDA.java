package jhn.eda;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

import jhn.eda.lucene.LuceneLabelAlphabet;
import jhn.eda.topicdistance.LuceneTopicDistanceCalculator;
import jhn.eda.topicdistance.TopicDistanceCalculator;
import jhn.eda.typetopiccounts.LuceneTypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.util.Util;

public final class RunEDA {
	private RunEDA() {}
	
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
		
		final String logFilename = logFilename(outputDir+"/runs");
		
		final String topicWordIndexName = "wp_lucene4"; /* "wp_lucene3" */
		
		final String indicesDir = outputDir + "/indices";
		final String topicWordIdxDir = indicesDir + "/topic_words/" + topicWordIndexName;
		final String artCatsIdxDir = indicesDir + "/article_categories";
		final String catCatsIdxDir = indicesDir + "/category_categories";
		
		
		final String datasetName = "debates2012";/* debates2012 */ /* toy_dataset2 */ /* state_of_the_union */
		final String datasetFilename = System.getenv("HOME") + "/Projects/eda/datasets/" + datasetName + ".mallet";
		
//		final String featselFilename = outputDir + "/featsel/tfidfTop10.ser";
//		System.out.print("Loading tf-idf features...");
//		
//		@SuppressWarnings("unchecked")
//		Set<String> tfidfTop10 = (Set<String>) Util.deserialize(featselFilename);
//		System.out.println("done.");
		
		
		File topicWordIdxDirF = new File(topicWordIdxDir);
		Directory dir = NIOFSDirectory.open(topicWordIdxDirF);
//		Directory dir = MMapDirectory.open(topicWordIdxDirF);
		IndexReader topicWordIdx = IndexReader.open(dir);

		LabelAlphabet topicAlphabet;
		if(LOAD_SERIALIZED_LABEL_ALPHABET) {
			final String alphaFilename = indicesDir + "/" + topicWordIndexName + "_label_alphabet.ser";
			System.out.print("Loading label alphabet...");
			topicAlphabet = (LabelAlphabet) Util.deserialize(alphaFilename);
			System.out.println("done.");
		} else {
			topicAlphabet = new LuceneLabelAlphabet(topicWordIdx);
		}
		
		System.out.print("Loading target corpus...");
		InstanceList targetData = InstanceList.load(new File(datasetFilename));
		System.out.println("done.");
		
		TypeTopicCounts ttcs = new LuceneTypeTopicCounts(topicWordIdx, targetData.getAlphabet());
		
		IndexReader articleCategoriesIdx = IndexReader.open(NIOFSDirectory.open(new File(artCatsIdxDir)));
		IndexReader categoryCategoriesIdx = IndexReader.open(NIOFSDirectory.open(new File(catCatsIdxDir)));
		TopicDistanceCalculator tdc = new LuceneTopicDistanceCalculator(articleCategoriesIdx, categoryCategoriesIdx);
		
		EDA eda = new EDA (ttcs, tdc, logFilename, topicAlphabet);
		
		// Cosmetic options:
		eda.config().putInt(Options.SHOW_TOPICS_INTERVAL, 1);
		
		// Algorithm options:
//		eda.config().putInt(Options.TYPE_TOPIC_MIN_COUNT, 3);
//		eda.config().putBool(Options.FILTER_DIGITS, true);
//		eda.config().putBool(Options.FILTER_MONTHS, true);
//		eda.config().putObj(Options.PRESELECTED_FEATURES, tfidfTop10);
		
		
		
		
		System.out.print("Processing target corpus...");
		eda.addInstances(targetData);
		System.out.println("done.");
		
		eda.sample(1000);
		
		topicWordIdx.close();
		articleCategoriesIdx.close();
		categoryCategoriesIdx.close();
		
	}//end main
}
