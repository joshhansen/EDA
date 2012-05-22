package jhn.eda;

import java.io.File;
import java.io.IOException;

import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.topicdistance.LuceneTopicDistanceCalculator;
import jhn.eda.topicdistance.TopicDistanceCalculator;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.util.ConstFactory;
import jhn.util.Factory;
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
		final String outputDir = Paths.outputDir();
		
		final String logFilename = logFilename(outputDir+"/runs");
		
		final String topicWordIdxName = "wp_lucene4"; /* "wp_lucene3" */
		
		final String datasetName = "debates2012";/* debates2012 */ /* toy_dataset2 */ /* state_of_the_union */
		final String datasetFilename = Paths.datasetFilename(datasetName);
		
//		final String artCatsIdxDir = Paths.indexDir("article_categories");
//		final String catCatsIdxDir = Paths.indexDir("category_categories");
		
		
		
		
        System.out.print("Loading target corpus...");
        InstanceList targetData = InstanceList.load(new File(datasetFilename));
        System.out.println("done.");

		System.out.print("Loading type-topic counts...");
		final int minCount = 2;
		final String ttCountsFilename = Paths.typeTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		TypeTopicCounts ttcs = (TypeTopicCounts) Util.deserialize(ttCountsFilename);
		System.out.println("done.");

		System.out.print("Loading label alphabet...");
//        String labelAlphabetFilename = Paths.topicWordIndicesDir() + "/" + topicWordIdxName + "_label_alphabet.ser";
		String labelAlphabetFilename = Paths.labelAlphabetFilename(topicWordIdxName, datasetName, minCount);
		LabelAlphabet topicAlphabet = (LabelAlphabet) Util.deserialize(labelAlphabetFilename);
		System.out.println("done.");

		System.out.print("Loading topic counts...");
		final String topicCountsFilename = Paths.topicCountsFilename(topicWordIdxName, datasetName, minCount);
		TopicCounts tcs = (TopicCounts) Util.deserialize(topicCountsFilename);
//		Factory<TopicCounts> tcFact = MapTopicCounts.factory(Paths.indexDir("topic_counts") + "/topic_counts.ser");
		Factory<TopicCounts> tcFact = new ConstFactory<TopicCounts>(tcs);
		System.out.println("done.");

		TopicDistanceCalculator tdc = new LuceneTopicDistanceCalculator(null, null);		
		EDA eda = new EDA (tcFact, ttcs, tdc, logFilename, topicAlphabet, 10000.0, 0.01);
		
		// Cosmetic options:
		eda.config().putBool(Options.PRINT_TOP_WORDS_AND_TOPICS, true);
		eda.config().putBool(Options.PRINT_DOC_TOPICS, true);
		eda.config().putInt(Options.SHOW_TOPICS_INTERVAL, 1);
		
		// Algorithm options:
//		eda.config().putInt(Options.TYPE_TOPIC_MIN_COUNT, 3);
//		eda.config().putBool(Options.FILTER_DIGITS, true);
//		eda.config().putBool(Options.FILTER_MONTHS, true);
//		eda.config().putObj(Options.PRESELECTED_FEATURES, tfidfTop10);
		
		eda.config().putInt(Options.TOPIC_MIN_COUNT, 100);
		
		
		System.out.print("Processing target corpus...");
		eda.addInstances(targetData);
		System.out.println("done.");
		
		eda.sample(1000);

//		articleCategoriesIdx.close();
//		categoryCategoriesIdx.close();

		
	}//end main
}
