package jhn.eda;

import java.io.File;
import java.io.IOException;

import cc.mallet.types.InstanceList;

import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.topicdistance.LuceneTopicDistanceCalculator;
import jhn.eda.topicdistance.TopicDistanceCalculator;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.util.Config;
import jhn.util.ConstFactory;
import jhn.util.Factory;
import jhn.util.Util;

public final class RunEDA {
	private RunEDA() {}
	
	public static final double DEFAULT_ALPHA_SUM = 50.0;
	public static final double DEFAULT_BETA = 0.01;
	public static void main (String[] args) throws IOException, ClassNotFoundException {
		final int iterations = 500;
		final int minCount = 2;
		final String topicWordIdxName = "wp_lucene4";
		final String datasetName = "reuters21578";// toy_dataset2 debates2012 sacred_texts state_of_the_union reuters21578
		
        System.out.print("Loading target corpus...");
        InstanceList targetData = InstanceList.load(new File(Paths.datasetFilename(datasetName)));
        System.out.println("done.");

		System.out.print("Loading type-topic counts...");
		final String ttCountsFilename = Paths.typeTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		TypeTopicCounts ttcs = (TypeTopicCounts) Util.deserialize(ttCountsFilename);
		System.out.println("done.");

//		System.out.print("Loading label alphabet...");
//		String labelAlphabetFilename = Paths.labelAlphabetFilename(topicWordIdxName, datasetName, minCount);
//		LabelAlphabet topicAlphabet = (LabelAlphabet) Util.deserialize(labelAlphabetFilename);
//		System.out.println("done.");

		System.out.print("Loading topic counts...");
//		final String topicCountsFilename = Paths.topicCountsFilename(topicWordIdxName, datasetName, minCount);
//		final String topicCountsFilename = Paths.restrictedTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		final String topicCountsFilename = Paths.filteredTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		TopicCounts tcs = (TopicCounts) Util.deserialize(topicCountsFilename);
		Factory<TopicCounts> tcFact = new ConstFactory<>(tcs);
		System.out.println("done.");

		Config props = (Config) Util.deserialize(Paths.propsFilename(topicWordIdxName, datasetName, minCount));
		
		TopicDistanceCalculator tdc = new LuceneTopicDistanceCalculator(null, null);		
		EDA eda = new EDA (tcFact, ttcs, tdc, props.getInt(Options.NUM_TOPICS), Paths.nextRun());
		
		// Cosmetic options:
//		eda.conf.putBool(Options.PRINT_TOP_DOC_TOPICS, true);
//		eda.conf.putBool(Options.PRINT_TOP_TOPIC_WORDS, true);
//		eda.conf.putBool(Options.PRINT_DOC_TOPICS, true);
		eda.conf.putInt(Options.PRINT_INTERVAL, 1);
//		eda.conf.putBool(Options.PRINT_REDUCED_DOCS, true);
//		eda.conf.putInt(Options.REDUCED_DOCS_TOP_N, 1);
		
		eda.conf.putBool(Options.PRINT_FAST_STATE, true);
		
		
//		eda.conf.putBool(Options.SERIALIZE_MODEL, true);
		
		// Algorithm options:
		eda.conf.putDouble(Options.ALPHA_SUM, 10000);
//		eda.conf.putDouble(Options.ALPHA_SUM, 10);
//		eda.conf.putInt(Options.ALPHA_OPTIMIZE_INTERVAL, 1);
//		eda.conf.putDouble(Options.ALPHA_SUM, DEFAULT_ALPHA_SUM);
		eda.conf.putDouble(Options.BETA, 0.01);
		eda.conf.putInt(Options.ITERATIONS, iterations);
		
		System.out.print("Processing target corpus...");
		eda.setTrainingData(targetData);
		System.out.println("done.");
		
		final int minThreads = Runtime.getRuntime().availableProcessors();
//		int minThreads = 1;
		final int maxThreads = Runtime.getRuntime().availableProcessors()*3;
//		int maxThreads = 2;
		eda.conf.putInt(Options.MIN_THREADS, minThreads);
		eda.conf.putInt(Options.MAX_THREADS, maxThreads);
		
		
		eda.sample();
	}
}
