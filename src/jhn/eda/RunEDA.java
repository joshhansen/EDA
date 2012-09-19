package jhn.eda;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import cc.mallet.types.InstanceList;

import jhn.eda.listeners.PrintFastState;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.topicdistance.ConstTopicDistanceCalculator;
import jhn.eda.topicdistance.TopicDistanceCalculator;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.util.Config;
import jhn.util.Util;


public class RunEDA {
	protected static final int PRINT_INTERVAL = 1;
	private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
	public static final double DEFAULT_ALPHA_SUM = 50.0;
	public static final double DEFAULT_BETA = 0.01;
	
	protected String runsDir;
	protected int run;
	protected int iterations;
	protected int minCount;
	protected String topicWordIdxName;
	protected String datasetName;
	protected InstanceList targetData;
	protected TypeTopicCounts ttcs;
	protected TopicCounts tcs;
	protected Config props;
	protected TopicDistanceCalculator tdc;
	
	public RunEDA() {
		runsDir = Paths.defaultRunsDir();
		iterations = 500;
		minCount = 2;
		topicWordIdxName = "wp_lucene4";
		datasetName = "reuters21578";// toy_dataset2 debates2012 sacred_texts state_of_the_union reuters21578
	}

	protected void loadAll() throws Exception {
		targetData = loadTargetData();
		ttcs = loadTypeTopicCounts();
		tcs = loadTopicCounts();
		props = loadProps();
	}
	
	protected void unloadAll() throws Exception {
		Util.closeIfPossible(targetData);
		Util.closeIfPossible(ttcs);
		Util.closeIfPossible(tcs);
		Util.closeIfPossible(props);
		tdc = loadTopicDistanceCalculator();
	}
	
	public void run() throws Exception {
		moveToNextRun();
		loadAll();
		runEDA();
		unloadAll();
	}

	protected void runEDA() throws FileNotFoundException, Exception {
		EDA eda = new EDA (tcs, ttcs, props.getInt(Options.NUM_TOPICS), runDir()+"/main.log");
		configure(eda.conf);
		addListeners(eda);
		processTargetData(eda);
		eda.sample();
	}
	
	protected void moveToNextRun() {
		run = nextRun();
	}
	
	protected int nextRun() {
		return Paths.nextRun(runsDir);
	}

	protected String runDir() {
		return Paths.runDir(runsDir, run);
	}

	protected Config loadProps() throws FileNotFoundException, IOException, ClassNotFoundException {
		return (Config) Util.deserialize(Paths.propsFilename(topicWordIdxName, datasetName, minCount));
	}

	@SuppressWarnings("static-method")
	protected ConstTopicDistanceCalculator loadTopicDistanceCalculator() {
		return new ConstTopicDistanceCalculator(0);
	}
	
	protected void configure(Config conf) {
		conf.putDouble(Options.ALPHA_SUM, 10000);
		conf.putDouble(Options.BETA, 0.01);
		conf.putInt(Options.ITERATIONS, iterations);
		conf.putInt(Options.MIN_THREADS, NUM_CORES);
		conf.putInt(Options.MAX_THREADS, NUM_CORES*3);
	}
	
	protected void addListeners(EDA eda) {
//		eda.addListener(new PrintState(PRINT_INTERVAL, runDir()));
		eda.addListener(new PrintFastState(PRINT_INTERVAL, runDir()));
//		eda.addListener(new PrintReducedDocsLibSVM(PRINT_INTERVAL, runDir()));
//		eda.addListener(new PrintReducedDocsLibSVM(PRINT_INTERVAL, runDir(), false));
//		eda.addListener(new PrintDocTopics(PRINT_INTERVAL, runDir()));
//		eda.addListener(new SerializeModel(PRINT_INTERVAL, runDir()));
//		eda.addListener(new PrintTopDocTopics(PRINT_INTERVAL, runDir(), 10));
//		eda.addListener(new PrintTopTopicWords(PRINT_INTERVAL, runDir(), 10));
	}
	
	protected void processTargetData(EDA eda) throws FileNotFoundException {
		System.out.print("Processing target corpus...");
		eda.setTrainingData(targetData);
		System.out.println("done.");
	}
	
	protected InstanceList loadTargetData() {
		System.out.print("Loading target corpus...");
		InstanceList theTargetData = InstanceList.load(new File(jhn.Paths.malletDatasetFilename(datasetName)));
		System.out.println("done.");
		return theTargetData;
	}
	
	protected TypeTopicCounts loadTypeTopicCounts() throws FileNotFoundException, IOException, ClassNotFoundException {
		System.out.print("Loading type-topic counts...");
		final String ttCountsFilename = Paths.typeTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		TypeTopicCounts theTTCs = (TypeTopicCounts) Util.deserialize(ttCountsFilename);
		System.out.println("done.");
		return theTTCs;
	}
	
	protected TopicCounts loadTopicCounts() throws FileNotFoundException, IOException, ClassNotFoundException {
		System.out.print("Loading topic counts...");
		final String topicCountsFilename = Paths.filteredTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		TopicCounts theTCs = (TopicCounts) Util.deserialize(topicCountsFilename);
		System.out.println("done.");
		return theTCs;
	}

	public static void main(String[] args) throws Exception {
		RunEDA runner = new RunEDA();
		runner.run();
	}
}
