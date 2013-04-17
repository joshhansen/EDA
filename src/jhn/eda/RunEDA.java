package jhn.eda;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;

import cc.mallet.types.InstanceList;

import jhn.ExtractorParams;
import jhn.eda.listeners.PrintFastState;
import jhn.eda.listeners.PrintFasterState;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.util.Config;
import jhn.util.Util;


public class RunEDA {
	private static final int PRINT_INTERVAL = 1;
	private static final int NUM_CORES = Runtime.getRuntime().availableProcessors();
	public static final double DEFAULT_ALPHA_SUM = 50.0;
	public static final double DEFAULT_BETA = 0.01;
	
	protected Class<? extends EDA> algo;
	protected String runsDir;
	protected int run;
	protected int iterations;
	protected boolean outputClass;
	protected ExtractorParams ep;
	protected InstanceList targetData;
	protected TypeTopicCounts ttcs;
	protected TopicCounts tcs;
	protected Config props;
	
	public RunEDA() {
		this(EDA2_1.class, Paths.defaultRunsDir(), 500, false, new ExtractorParams("wp_lucene4", "sotu_chunks"/*"toy_dataset4"*/, 2));// "reuters21578_noblah2");// toy_dataset2 debates2012 sacred_texts state_of_the_union reuters21578
	}
	
	public RunEDA(Class<? extends EDA> algo, String runsDir, int iterations, boolean outputClass, ExtractorParams ep) {
		this.algo = algo;
		this.runsDir = runsDir;
		this.iterations = iterations;
		this.outputClass = outputClass;
		this.ep = ep;
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
	}
	
	public void run() throws Exception {
		moveToNextRun();
		loadAll();
		runEDA();
		unloadAll();
	}

	protected void runEDA() throws FileNotFoundException, Exception {
		new File(runDir()).mkdirs();
		
		for(Constructor<?> ctor : algo.getDeclaredConstructors()) {
			System.out.println(ctor);
		}
		
		EDA eda = algo.getConstructor(TopicCounts.class, TypeTopicCounts.class, Integer.TYPE, String.class)
				.newInstance(tcs, ttcs, Integer.valueOf(props.getInt(Options.NUM_TOPICS)), runDir()+"/main.log");
		
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
		return (Config) Util.deserialize(jhn.Paths.propsFilename(ep));
	}
	
	protected void configure(Config conf) {
		conf.putDouble(Options.ALPHA_SUM, 100);
//		conf.putInt(Options.ALPHA_OPTIMIZE_INTERVAL, 1);
		conf.putDouble(Options.BETA, 0.01);
		conf.putInt(Options.ITERATIONS, iterations);
		conf.putInt(Options.MIN_THREADS, NUM_CORES);
		conf.putInt(Options.MAX_THREADS, NUM_CORES);
	}
	
	protected void addListeners(EDA eda) throws Exception {
//		eda.addListener(new PrintState(PRINT_INTERVAL, runDir()));
//		eda.addListener(new PrintFastState(PRINT_INTERVAL, runDir(), outputClass));
		eda.addListener(new PrintFasterState(PRINT_INTERVAL, runDir(), outputClass));
//		eda.addListener(new PrintReducedDocsLibSVM(PRINT_INTERVAL, runDir()));
//		eda.addListener(new PrintReducedDocsLibSVM(PRINT_INTERVAL, runDir(), false));
//		eda.addListener(new PrintDocTopics(PRINT_INTERVAL, runDir()));
//		eda.addListener(new SerializeModel(PRINT_INTERVAL, runDir()));
//		eda.addListener(new PrintTopDocTopics(PRINT_INTERVAL, runDir(), 10));
//		eda.addListener(new PrintTopTopicWords(PRINT_INTERVAL, runDir(), 10));
	}
	
	protected void processTargetData(EDA eda) {
		System.out.print("Processing target corpus...");
		eda.setTrainingData(targetData);
		System.out.println("done.");
	}
	
	protected InstanceList loadTargetData() {
		System.out.print("Loading target corpus...");
		InstanceList theTargetData = InstanceList.load(new File(jhn.Paths.malletDatasetFilename(ep.datasetName)));
		System.out.println("done.");
		return theTargetData;
	}
	
	protected TypeTopicCounts loadTypeTopicCounts() throws FileNotFoundException, IOException, ClassNotFoundException {
		System.out.print("Loading type-topic counts...");
		final String ttCountsFilename = jhn.Paths.typeTopicCountsFilename(ep);
		TypeTopicCounts theTTCs = (TypeTopicCounts) Util.deserialize(ttCountsFilename);
		System.out.println("done.");
		return theTTCs;
	}
	
	protected TopicCounts loadTopicCounts() throws FileNotFoundException, IOException, ClassNotFoundException {
		System.out.print("Loading topic counts...");
		final String topicCountsFilename = jhn.Paths.filteredTopicCountsFilename(ep);
		TopicCounts theTCs = (TopicCounts) Util.deserialize(topicCountsFilename);
		System.out.println("done.");
		return theTCs;
	}

	public static void main(String[] args) throws Exception {
		RunEDA runner = new RunEDA();
		runner.run();
	}
}
