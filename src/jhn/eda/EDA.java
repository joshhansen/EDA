/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.	For further
information, see the file `LICENSE' included with this distribution. */
package jhn.eda;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import jhn.counts.Counter;
import jhn.counts.i.i.IntIntCounter;
import jhn.counts.i.i.IntIntRAMCounter;
import jhn.counts.i.i.i.IntIntIntCounterMap;
import jhn.counts.i.i.i.IntIntIntRAMCounterMap;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.topiccounts.TopicCountsException;
import jhn.eda.topicdistance.MaxTopicDistanceCalculator;
import jhn.eda.topicdistance.StandardMaxTopicDistanceCalculator;
import jhn.eda.topicdistance.TopicDistanceCalculator;
import jhn.eda.typetopiccounts.TypeTopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCountsException;
import jhn.idx.Index;
import jhn.idx.RAMIndex;
import jhn.util.Config;
import jhn.util.Factory;
import jhn.util.Log;
import jhn.util.Util;

/**
* An implementation of Explicit Dirichlet Allocation using Gibbs sampling. Based on SimpleLDA by David Mimno and Andrew
* McCallum.
* 
* @author Josh Hansen
*/
public class EDA implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected final int numTopics;
	protected int numDocs;
	protected int[][] tokens;
	protected int[][] topics;
	protected int[] docLengths;
	protected String[] docNames;
	protected double[] alphas;
	
	// Classification helpers
	protected String[] docLabels;
	protected Index<String> allLabels;
	
	protected final String logDir;
	protected final int run;
	protected transient Log log;
	public final Config conf = new Config();
	protected Randoms random;
	
	// Data sources and other helpers
	protected transient TypeTopicCounts typeTopicCounts;
	protected transient TopicDistanceCalculator topicDistCalc;
	protected transient MaxTopicDistanceCalculator maxTopicDistCalc = new StandardMaxTopicDistanceCalculator();
	protected transient Factory<TopicCounts> topicCountsFact;
	
	public EDA (Factory<TopicCounts> topicCountsFact, TypeTopicCounts typeTopicCounts,
			TopicDistanceCalculator topicDistCalc, final int numTopics, final int run) {
		this(topicCountsFact, typeTopicCounts, topicDistCalc, numTopics, run, new Randoms());
	}
	
	public EDA(Factory<TopicCounts> topicCountsFact, TypeTopicCounts typeTopicCounts, TopicDistanceCalculator topicDistCalc,
			final int numTopics, final int run, Randoms random) {
		
		this.topicCountsFact = topicCountsFact;
		this.typeTopicCounts = typeTopicCounts;
		this.topicDistCalc = topicDistCalc;
		this.random = random;
		
		this.numTopics = numTopics;
		conf.putInt(Options.NUM_TOPICS, numTopics);
		
		this.logDir = Paths.runDir(run);
		this.run = run;
	}
	
	private void initLogging() throws FileNotFoundException {
		File logFile = new File(logDir);
		if(!logFile.exists()) {
			logFile.mkdirs();
		}
		
		log = new Log(System.out, logDir + "/main.log");
		log.println(EDA.class.getName() + ": " + numTopics + " topics");
		log.println("Topic Counts Source: " + typeTopicCounts.getClass().getSimpleName());
		log.println("Topic Distance Calc: " + topicDistCalc.getClass().getSimpleName());
		log.println("Max Topic Distance Calc: " + maxTopicDistCalc.getClass().getSimpleName());
		
		Object[][] outputs = new Object[][] {
				{Options.PRINT_DOC_TOPICS, "doctopics"},
				{Options.PRINT_STATE, "state"},
				{Options.PRINT_REDUCED_DOCS, "reduced"},
				{Options.PRINT_TOP_DOC_TOPICS, "top_doc_topics"},
				{Options.PRINT_TOP_TOPIC_WORDS, "top_topic_words"},
				{Options.SERIALIZE_MODEL, "model"},
				{Options.PRINT_FAST_STATE, "fast_state"}
		};
		
		for(Object[] output : outputs) {
			if(conf.isTrue((Enum<?>)output[0])) {
				File f = new File(logDir + "/" + output[1]);
				if(!f.exists()) {
					f.mkdirs();
				}
			}
		}
	}

	public void setTrainingData (InstanceList training) throws FileNotFoundException {
		initLogging();
		
		
		numDocs = training.size();
		
		log.println("Dataset instances: " + training.size());
		
		final int numTypes = training.getDataAlphabet().size();
		conf.putInt(Options.NUM_TYPES, numTypes);
		
		log.print("Loading: ");
		int tokenCount = 0;
		int docLength;
		tokens = new int[numDocs][];
		topics = new int[numDocs][];
		docLengths = new int[numDocs];
		docNames = new String[numDocs];
		
		docLabels = new String[numDocs];
		SortedSet<String> labels = new TreeSet<>();
		
		Instance instance;
		String label;
		for(int docNum = 0; docNum < numDocs; docNum++) {
			instance = training.get(docNum);
			docNames[docNum] = instance.getName().toString();
			tokens[docNum] = ((FeatureSequence) instance.getData()).getFeatures();
			docLength = tokens[docNum].length;
			docLengths[docNum] = docLength;
			
			label = instance.getTarget().toString();
			docLabels[docNum] = label;
			labels.add(label);
			
			topics[docNum] = new int[docLength];
			for (int position = 0; position < docLength; position++) {
				topics[docNum][position] = random.nextInt(numTopics);
			}
			
			log.print(instance.getSource());
			log.print(" ");
			
			tokenCount += docLength;
		}
		
		allLabels = new RAMIndex<>();
		allLabels.indexOf("none");//Needed for use in SparseInstance
		for(String theLabel : labels) {
			allLabels.indexOf(theLabel);
		}
		
		log.println();
		
		log.println("Loaded " + tokenCount + " tokens.");
	}

	public void sample () {
		// Compute alpha from alphaSum
		final double startingAlpha = conf.getDouble(Options.ALPHA_SUM) / conf.getInt(Options.NUM_TOPICS);
		conf.putDouble(Options.ALPHA, startingAlpha);
		alphas = new double[numTopics];
		Arrays.fill(alphas, startingAlpha);
		
		// Compute betaSum from beta
		final double betaSum = conf.getDouble(Options.BETA) * conf.getInt(Options.NUM_TYPES);
		conf.putDouble(Options.BETA_SUM, betaSum);
		
		final int iterations = conf.getInt(Options.ITERATIONS);
		log.println("Going to sample " + iterations + " iterations with configuration:");
		log.println(conf.toString(1));
		
		final int minThreads = conf.getInt(Options.MIN_THREADS);
		final int maxThreads = conf.getInt(Options.MAX_THREADS);
		
		for (int iteration = 1; iteration <= iterations; iteration++) {			
			log.println("Iteration " + iteration);
			long iterationStart = System.currentTimeMillis();
			
			final double maxTopicDistance = maxTopicDistCalc.maxTopicDistance(iteration, iterations);
			
			BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
			ThreadPoolExecutor exec = new ThreadPoolExecutor(minThreads, maxThreads, 500L, TimeUnit.MILLISECONDS, queue);
			
			// Loop over every document in the corpus
			for (int docNum = 0; docNum < numDocs; docNum++) {
				exec.execute(new DocumentSampler(docNum, maxTopicDistance, topicCountsFact.create()));
			}
			
			exec.shutdown();
			
			try {
				while(!exec.awaitTermination(500L, TimeUnit.MILLISECONDS)) {
					// Do nothing
				}
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		
			long elapsedMillis = System.currentTimeMillis() - iterationStart;
			log.println("Iteration " + iteration + " duration: " + elapsedMillis + "ms\t");
			
			if(iteration % conf.getInt(Options.PRINT_INTERVAL) == 0) {
				log.println("Printing output");
				if(conf.isTrue(Options.PRINT_TOP_DOC_TOPICS) || conf.isTrue(Options.PRINT_TOP_TOPIC_WORDS)) {
					printTopWordsAndTopics(iteration, 100, 10);
				}
				if(conf.isTrue(Options.PRINT_DOC_TOPICS)) {
					try {
						PrintStream out = new PrintStream(new FileOutputStream(logDir + "/doctopics/" + iteration + ".log"));
						printDocumentTopics(out, 0.01, 100);
						out.close();
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
				
				
				if(conf.isTrue(Options.PRINT_STATE)) {
					try {
						PrintStream out = new PrintStream(new FileOutputStream(logDir + "/state/" + iteration + ".state"));
						printState(out);
						out.close();
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
				
				if(conf.isTrue(Options.PRINT_FAST_STATE)) {
					try {
						PrintStream out = new PrintStream(new FileOutputStream(Paths.fastStateFilename(run, iteration)));
						printFastState(out);
						out.close();
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
				

				if(conf.isTrue(Options.PRINT_LOG_LIKELIHOOD)) {
					log.println("<" + iteration + "> Log Likelihood: " + modelLogLikelihood());
				}
				
				if(conf.isTrue(Options.PRINT_REDUCED_DOCS)) {
					try {
						PrintStream out = new PrintStream(new FileOutputStream(logDir + "/reduced/" + iteration + ".libsvm"));
						printReducedDocsLibSvm(out);
						out.close();
					} catch(IOException e) {
						e.printStackTrace();
					}
					
					try {
						PrintStream out = new PrintStream(new FileOutputStream(logDir + "/reduced/" + iteration + ".libsvm_unnorm"));
						printReducedDocsLibSvm(out, false);
						out.close();
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
				
				if(conf.isTrue(Options.SERIALIZE_MODEL)) {
					Util.serialize(this, logDir + "/model/" + iteration + ".ser");
				}
				
				log.println();
			}
		}
		
		log.close();
	}

	private int[] docTopicCounts(final int docNum) {
		int[] docTopicCounts = new int[numTopics];
		docTopicCounts(docNum, docTopicCounts);
		return docTopicCounts;
	}
	
	private IntIntCounter docTopicCounter(final int docNum) {
		IntIntCounter counts = new IntIntRAMCounter();
		for(int topic : topics[docNum]) {
			counts.inc(topic);
		}
		return counts;
	}
	
	private void docTopicCounts(final int docNum, final int[] topicCounts) {
		for(int topic : topics[docNum]) {
			topicCounts[topic]++;
		}
	}
	
	private class DocumentSampler implements Runnable {
		private final int docNum;
		private final double maxTopicDistance;
		private TopicCounts topicCounts;
		public DocumentSampler(int docNum, double maxTopicDistance, TopicCounts topicCounts) {
			this.docNum = docNum;
			this.maxTopicDistance = maxTopicDistance;
			this.topicCounts = topicCounts;
		}
		
		@Override
		public void run() {
			final double beta = conf.getDouble(Options.BETA);
			final double betaSum = conf.getDouble(Options.BETA_SUM);
			
			int typeIdx, oldTopic, newTopic;
			int docLength = docLengths[docNum];

			int[] docTopicCounts = docTopicCounts(this.docNum);
			
			IntList ccTopics = new IntArrayList();
			DoubleList ccScores = new DoubleArrayList();
			
			int i;
			int topicCount;
			double score, sum, sample, countDelta;
			TypeTopicCount ttc;
			Iterator<TypeTopicCount> ttcIt;
			boolean topicInRange;
			
			//	Iterate over the positions (words) in the document 
			for (int position = 0; position < docLength; position++) {
				typeIdx = tokens[docNum][position];
				
				if(position > 0) {
					ccTopics.clear();
					ccScores.clear();
				}
				
				try {
					oldTopic = topics[docNum][position];
		
					// Now calculate and add up the scores for each topic for this word
					sum = 0.0;
		
					// Here's where the math happens! Note that overall performance is 
					//  dominated by what you do in this loop.
					ttcIt = typeTopicCounts.typeTopicCounts(typeIdx);
					while(ttcIt.hasNext()) {
						ttc = ttcIt.next();
						
						topicInRange = topicDistCalc.topicDistance(oldTopic, ttc.topic) <= maxTopicDistance;
						
						if(topicInRange) {
							topicCount = topicCounts.topicCount(ttc.topic);
							
							countDelta = ttc.topic==oldTopic ? 1.0 : 0.0;
							score = (alphas[ttc.topic] + docTopicCounts[ttc.topic] - countDelta) *
									(beta + ttc.count) /
									(betaSum + topicCount - countDelta);
							
							sum += score;
							
							ccTopics.add(ttc.topic);
							ccScores.add(score);
						}
					}
					
					if(sum <= 0.0) {
//						String type = alphabet.lookupObject(typeIdx).toString();
//						System.err.println("No instances of '" + type + "' (" + typeIdx + ") in topic corpus");
					} else {
						// Choose a random point between 0 and the sum of all topic scores
						sample = random.nextUniform() * sum;
			
						// Figure out which topic contains that point
						i = -1;
						newTopic = -1;
						while (sample > 0.0) {
							i++;
							newTopic = ccTopics.getInt(i);
							sample -= ccScores.getDouble(i);
						}
			
						// Make sure we actually sampled a topic
						if (newTopic == -1) {
							throw new IllegalStateException (EDA.class.getName()+": New topic not sampled.");
						}
		
						// Put the new topic in place
						topics[docNum][position] = newTopic;
					}
				} catch(TypeTopicCountsException e) {
					// Words that occur in none of the topics will lead us here
//					System.err.print(alphabet.lookupObject(typeIdx).toString() + " ");
					System.err.print(typeIdx);
					System.err.print(' ');
				} catch(TopicCountsException e) {
					e.printStackTrace();
				}
			}//end for position
			
			if(topicCounts instanceof Closeable) {
				try {
					((Closeable)topicCounts).close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new IllegalArgumentException();
				}
			}
			
			samplerFinished();
		}
		
		private void samplerFinished() {
			log.print('.');
			if(docNum > 0 && docNum % 120 == 0) {
				log.println(docNum);
			}
		}
	}//end class DocumentSampler
	

	
	/**
	 * The likelihood of the model is a combination of a Dirichlet-multinomial
	 * for the words in each topic and a Dirichlet-multinomial for the topics in
	 * each document.
	 * 
	 * The likelihood function of a dirichlet multinomial is Gamma( sum_i
	 * alpha_i ) prod_i Gamma( alpha_i + N_i ) prod_i Gamma( alpha_i ) Gamma(
	 * sum_i (alpha_i + N_i) )
	 * 
	 * So the log likelihood is logGamma ( sum_i alpha_i ) - logGamma ( sum_i
	 * (alpha_i + N_i) ) + sum_i [ logGamma( alpha_i + N_i) - logGamma( alpha_i
	 * ) ]
	 */
	public double modelLogLikelihood() {
		final double alpha = conf.getDouble(Options.ALPHA);
		final double alphaSum = conf.getDouble(Options.ALPHA_SUM);
		final double beta = conf.getDouble(Options.BETA);
		final int numTypes = conf.getInt(Options.NUM_TYPES);
		
		double logLikelihood = 0.0;

		// Do the documents first
		int[] topicCounts = new int[numTopics];
		double[] topicLogGammas = new double[numTopics];

		for (int topic=0; topic < numTopics; topic++) {
			topicLogGammas[ topic ] = Dirichlet.logGamma( alpha );
		}
	
		for (int docNum=0; docNum < numDocs; docNum++) {
			for(int topic : topics[docNum]) {
				topicCounts[topic]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCounts[topic] > 0) {
					logLikelihood += (Dirichlet.logGamma(alpha + topicCounts[topic]) -
									  topicLogGammas[ topic ]);
				}
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGamma(alphaSum + docLengths[docNum]);

			Arrays.fill(topicCounts, 0);
		}
	
		// add the parameter sum term
		logLikelihood += numDocs * Dirichlet.logGamma(alphaSum);

		// Count the number of type-topic pairs
		int nonZeroTypeTopics = 0;

		for (int type=0; type < numTypes; type++) {
			try {
				Iterator<TypeTopicCount> tcIt = typeTopicCounts.typeTopicCounts(type);
				while(tcIt.hasNext()) {
					TypeTopicCount tc = tcIt.next();
					nonZeroTypeTopics++;
					logLikelihood += Dirichlet.logGamma(beta + tc.count);
					if (Double.isNaN(logLikelihood)) {
						log.println(tc.count);
						System.exit(1);
					}
				}
			} catch(TypeTopicCountsException e) {
				// Words that occur in none of the topics will lead us here
				// Do nothing
			}
		}
	
		//FIXME
//		for (int topic=0; topic < numTopics; topic++) {
//			logLikelihood -= 
//				Dirichlet.logGamma( (beta * numTopics) +
//											tokensPerTopic[ topic ] );
//			if (Double.isNaN(logLikelihood)) {
//				log.println("after topic " + topic + " " + tokensPerTopic[ topic ]);
//				System.exit(1);
//			}
//
//		}
	
		logLikelihood += 
			(Dirichlet.logGamma(beta * numTopics)) -
			(Dirichlet.logGamma(beta) * nonZeroTypeTopics);

		if (Double.isNaN(logLikelihood)) {
			log.println("at the end");
			System.exit(1);
		}


		return logLikelihood;
	}

	// 
	// Methods for displaying and saving results
	//

	private static final Comparator<Entry<Integer,Counter<Integer,Integer>>> counterCmp = new Comparator<Entry<Integer,Counter<Integer,Integer>>>(){
		@Override
		public int compare(Entry<Integer, Counter<Integer,Integer>> o1, Entry<Integer, Counter<Integer,Integer>> o2) {
			return o2.getValue().totalCount().compareTo(o1.getValue().totalCount());
		}
	};
	
	private void printTopWordsAndTopics(int iteration, int numTopicsToPrint, int numWords) {
		log.print("Counting");
		IntIntIntCounterMap docTopicCounts = new IntIntIntRAMCounterMap();
		IntIntIntCounterMap topicWordCounts = new IntIntIntRAMCounterMap();
		
		final boolean printTopWords = conf.isTrue(Options.PRINT_TOP_TOPIC_WORDS);
		final boolean printTopTopics = conf.isTrue(Options.PRINT_TOP_DOC_TOPICS);
		for(int docNum = 0; docNum < numDocs; docNum++) {
			for(int i = 0; i < docLengths[docNum]; i++) {
				if(printTopWords) {
					topicWordCounts.inc(topics[docNum][i], tokens[docNum][i]);
				}
				if(printTopTopics) {
					docTopicCounts.inc(docNum, topics[docNum][i]);
				}
			}
			log.print('.');
		}
		log.println();
		
		if(printTopWords) {
			try {
				PrintStream out = new PrintStream(new FileOutputStream(logDir + "/top_topic_words/" + iteration + ".log"));
				printTopTopicWords(topicWordCounts, out, numTopicsToPrint, numWords);
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		if(printTopTopics) {
			try {
				PrintStream out = new PrintStream(new FileOutputStream(logDir + "/top_doc_topics/" + iteration + ".log"));
				printTopDocTopics(docTopicCounts, out, numWords);
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void printTopTopicWords(IntIntIntCounterMap topicWordCounts, PrintStream out, int numTopics, int numWords) {
		out.println("Topic words:");
		List<Entry<Integer,Counter<Integer,Integer>>> topicWordCounters = new ArrayList<>(topicWordCounts.entrySet());
		Collections.sort(topicWordCounters, counterCmp);
		
		for(Entry<Integer,Counter<Integer,Integer>> counterEntry : topicWordCounters.subList(0, numTopics)) {
			out.print("#");
			out.print(counterEntry.getKey());
			out.print(" \"");
			
			//FIXME
//			out.print(topicAlphabet.lookupObject(counterEntry.getKey()));
			out.print(counterEntry.getKey());
			
			out.print("\" [total=");
			out.print(counterEntry.getValue().totalCount());
			out.println("]:");
			out.print('\t');
			
			for(Entry<Integer,Integer> countEntry : counterEntry.getValue().topN(numWords)) {
				Integer typeIdx = countEntry.getKey();
				Integer typeCount = countEntry.getValue();
				
				//FIXME
//				String type = alphabet.lookupObject(typeIdx).toString();
//				log.print('\t');
//				out.print(type);
				out.print(typeIdx);
				
				out.print("[");
				out.print(typeCount);
				out.print("] ");
			}
			out.println();
			out.println();
		}
	}
	
	private void printTopDocTopics(IntIntIntCounterMap docTopicCounts, PrintStream out, int numWords) {
		out.println("Documents topics:");
		
		for(int docNum = 0; docNum < numDocs; docNum++) {
			IntIntCounter counter = docTopicCounts.getCounter(docNum);
			
			out.print(docNames[docNum]);
			out.print(" [total=");
			out.print(counter.totalCount());
			out.println("]:");
			
			for(Int2IntMap.Entry countEntry : counter.fastTopN(numWords)) {
				int topicIdx = countEntry.getIntKey();
				int typeCount = countEntry.getIntValue();
				out.print("\t");
				
				//FIXME
//				String topicLabel = topicAlphabet.lookupObject(topicIdx).toString();
//				out.print(topicLabel);
				out.print(topicIdx);
				out.print("[");
				out.print(typeCount);
				out.println("]");
			}
			out.println();
		}
	}


	/**
	 *  @param file        The filename to print to
	 *  @param threshold   Only print topics with proportion greater than this number
	 *  @param max         Print no more than this many topics
	 */
	private void printDocumentTopics (PrintStream out, double threshold, int max) {
		if(max < 1) throw new IllegalArgumentException("Max must be 1 or greater");
		
		out.print ("#doc source topic proportion ...\n");
		int[] topicCounts = new int[ numTopics ];

		IDSorter[] sortedTopics = new IDSorter[ numTopics ];
		for (int topic = 0; topic < numTopics; topic++) {
			// Initialize the sorters with dummy values
			sortedTopics[topic] = new IDSorter(topic, topic);
		}

		for (int docNum = 0; docNum < numDocs; docNum++) {
			out.print (docNum); out.print (' ');
			out.print(docNames[docNum]);
			out.print (' ');

			topicCounts = docTopicCounts(docNum);

			// And normalize
			for (int topic = 0; topic < numTopics; topic++) {
				sortedTopics[topic].set(topic, (double) topicCounts[topic] / (double) docLengths[docNum]);
			}
			
			Arrays.sort(sortedTopics);

			for (int i = 0; i < Math.min(max, numTopics); i++) {
				if (sortedTopics[i].getWeight() < threshold) { break; }
				
				out.print (sortedTopics[i].getID() + " " + 
						  sortedTopics[i].getWeight() + " ");
			}
			out.print (" \n");

			Arrays.fill(topicCounts, 0);
		}
		
	}
	
	private void printState (PrintStream out) {
//		out.println ("#doc source pos typeindex type topic");
		out.println("#doc source pos typeindex topic");
		for (int docNum = 0; docNum < numDocs; docNum++) {
			for (int position = 0; position < docLengths[docNum]; position++) {
				out.print(docNum); out.print(' ');
				out.print(docNames[docNum]); out.print(' '); 
				out.print(position); out.print(' ');
				out.print(tokens[docNum][position]); out.print(' ');
//				out.print(alphabet.lookupObject(tokens[docNum][position])); out.print(' ');
				out.print(topics[docNum][position]); out.println();
			}
		}
	}
	
	private void printFastState(PrintStream out) {
		out.println ("#docnum class source token1topic token2topic ... tokenNtopic");
		for (int docNum = 0; docNum < numDocs; docNum++) {
			out.print(docNum);
			out.print(' ');
			out.print(allLabels.indexOf(docLabels[docNum], false));
			out.print(' ');
			out.print(docNames[docNum]);
			for (int position = 0; position < docLengths[docNum]; position++) {
				out.print(' ');
				out.print(topics[docNum][position]);
			}
			out.println();
		}
	}
	
	public static final Comparator<Int2IntMap.Entry> fastKeyCmp = new Comparator<Int2IntMap.Entry>(){
		@Override
		public int compare(Int2IntMap.Entry o1, Int2IntMap.Entry o2) {
			return Util.compareInts(o1.getIntKey(), o2.getIntKey());
		}
	};
	
	private void printReducedDocsLibSvm(PrintStream out) {
		printReducedDocsLibSvm(out, true);
	}
	
	private void printReducedDocsLibSvm(PrintStream out, boolean normalize) {
		int classNum;
		IntIntCounter docTopicCounts;
		double docLength;
		for(int docNum = 0; docNum < numDocs; docNum++) {
			classNum = allLabels.indexOf(docLabels[docNum], false);
			docTopicCounts = docTopicCounter(docNum);
			docLength = docLengths[docNum];
			
			out.print(classNum);
			
			Int2IntMap.Entry[] entries = docTopicCounts.int2IntEntrySet().toArray(new Int2IntMap.Entry[0]);
			Arrays.sort(entries, fastKeyCmp);
			
			for(Int2IntMap.Entry entry : entries) {
				out.print(' ');
				out.print(entry.getIntKey());
				out.print(':');
				if(normalize) {
					out.print( entry.getIntValue() / docLength);
				} else {
					out.print(entry.getIntValue());
				}
			}
			out.println();
		}
	}
}
