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
	
	private static String logFilename() {
		final String logDir = Paths.runsDir();
		String filename = logDir + "/" + String.valueOf(nextLogNum(logDir)) + ".txt";
		System.out.println("Writing to log file: " + filename);
		return filename;
	}
	
	public static final double DEFAULT_ALPHA_SUM = 50.0;
	public static final double DEFAULT_BETA = 0.01;
	public static void main (String[] args) throws IOException, ClassNotFoundException {
		final int iterations = 500;
		final int minCount = 2;
		final String topicWordIdxName = "wp_lucene4";
		final String datasetName = "state_of_the_union";// toy_dataset2, debates2012, state_of_the_union
		
        System.out.print("Loading target corpus...");
        InstanceList targetData = InstanceList.load(new File(Paths.datasetFilename(datasetName)));
        System.out.println("done.");

		System.out.print("Loading type-topic counts...");
		final String ttCountsFilename = Paths.typeTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		TypeTopicCounts ttcs = (TypeTopicCounts) Util.deserialize(ttCountsFilename);
		System.out.println("done.");

		System.out.print("Loading label alphabet...");
		String labelAlphabetFilename = Paths.labelAlphabetFilename(topicWordIdxName, datasetName, minCount);
		LabelAlphabet topicAlphabet = (LabelAlphabet) Util.deserialize(labelAlphabetFilename);
		System.out.println("done.");

		System.out.print("Loading topic counts...");
//		final String topicCountsFilename = Paths.topicCountsFilename(topicWordIdxName, datasetName, minCount);
//		final String topicCountsFilename = Paths.restrictedTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		final String topicCountsFilename = Paths.filteredTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		TopicCounts tcs = (TopicCounts) Util.deserialize(topicCountsFilename);
		Factory<TopicCounts> tcFact = new ConstFactory<TopicCounts>(tcs);
		System.out.println("done.");

		TopicDistanceCalculator tdc = new LuceneTopicDistanceCalculator(null, null);		
		EDA eda = new EDA (tcFact, ttcs, tdc, logFilename(), topicAlphabet);
		
		// Cosmetic options:
		eda.config().putBool(Options.PRINT_TOP_WORDS_AND_TOPICS, true);
//		eda.config().putBool(Options.PRINT_DOC_TOPICS, true);
		eda.config().putInt(Options.PRINT_INTERVAL, 1);
		
		// Algorithm options:
//		eda.config().putDouble(Options.ALPHA_SUM, 10000);
		eda.config().putDouble(Options.ALPHA_SUM, 1000);
//		eda.config().putDouble(Options.ALPHA_SUM, DEFAULT_ALPHA_SUM);
		eda.config().putDouble(Options.BETA, 0.01);
		eda.config().putInt(Options.ITERATIONS, iterations);
		
		System.out.print("Processing target corpus...");
		eda.setTrainingData(targetData);
		System.out.println("done.");
		
		eda.sample();
		
	}
}
