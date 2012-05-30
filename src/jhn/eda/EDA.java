/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.	For further
information, see the file `LICENSE' included with this distribution. */
package jhn.eda;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cc.mallet.types.Alphabet;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.Randoms;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.topiccounts.TopicCountsException;
import jhn.eda.topicdistance.MaxTopicDistanceCalculator;
import jhn.eda.topicdistance.StandardMaxTopicDistanceCalculator;
import jhn.eda.topicdistance.TopicDistanceCalculator;
import jhn.eda.typetopiccounts.TypeTopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCountsException;
import jhn.util.Config;
import jhn.util.Counter;
import jhn.util.CounterMap;
import jhn.util.Factory;
import jhn.util.IntIntCounter;
import jhn.util.IntIntIntCounterMap;
import jhn.util.Log;

/**
* An implementation of Explicit Dirichlet Allocation using Gibbs sampling. Based on SimpleLDA by David Mimno and Andrew
* McCallum.
* 
* @author Josh Hansen
*/
public class EDA {
	protected final int numTopics;
	protected int numDocs;
	protected int[][] tokens;
	protected int[][] topics;
	protected int[] docLengths;
	protected String[] docNames;
	protected double[] alphas;

	// the alphabet for the input data
	protected Alphabet alphabet;
	
	// the alphabet for the topics
	protected LabelAlphabet topicAlphabet;
	
	protected final Log log;
	protected Log docTopicsLog;
	protected Log stateLog;
	protected Config conf = new Config();
	protected Randoms random;
	
	// Data sources and other helpers
	protected TypeTopicCounts typeTopicCounts;
	protected TopicDistanceCalculator topicDistCalc;
	protected MaxTopicDistanceCalculator maxTopicDistCalc = new StandardMaxTopicDistanceCalculator();
	protected Factory<TopicCounts> topicCountsFact;
	
	public EDA (Factory<TopicCounts> topicCountsFact, TypeTopicCounts typeTopicCounts, TopicDistanceCalculator topicDistCalc, String logFilename, LabelAlphabet topicAlphabet) throws FileNotFoundException {
		this(topicCountsFact, typeTopicCounts, topicDistCalc, logFilename, topicAlphabet, new Randoms());
	}
	
	public EDA(Factory<TopicCounts> topicCountsFact, TypeTopicCounts typeTopicCounts, TopicDistanceCalculator topicDistCalc,
			final String logFilename, final LabelAlphabet topicAlphabet, Randoms random) throws FileNotFoundException {
		
		this.topicCountsFact = topicCountsFact;
		this.typeTopicCounts = typeTopicCounts;
		this.topicDistCalc = topicDistCalc;
		this.topicAlphabet = topicAlphabet;
		this.random = random;
		
		numTopics = topicAlphabet.size();
		conf.putInt(Options.NUM_TOPICS, numTopics);
		
		// Start logging
		log = new Log(System.out, logFilename);
		log.println(EDA.class.getName() + ": " + numTopics + " topics");
		log.println("Topic Counts Source: " + typeTopicCounts.getClass().getSimpleName());
		log.println("Topic Distance Calc: " + topicDistCalc.getClass().getSimpleName());
		log.println("Max Topic Distance Calc: " + maxTopicDistCalc.getClass().getSimpleName());
		
		if(conf.isTrue(Options.PRINT_DOC_TOPICS)) {
			docTopicsLog = new Log(logFilename+".doctopics");
		}
		if(conf.isTrue(Options.PRINT_STATE)) {
			stateLog = new Log(logFilename+".state");
		}
	}
	
	public Config config() {
		return conf;
	}

	public void setTrainingData (InstanceList training) {
		numDocs = training.size();
		
		log.println("Dataset instances: " + training.size());
		
		alphabet = training.getDataAlphabet();
		
		final int numTypes = alphabet.size();
		conf.putInt(Options.NUM_TYPES, numTypes);
		
		log.print("Loading: ");
		int tokenCount = 0;
		int docLength;
		tokens = new int[numDocs][];
		topics = new int[numDocs][];
		docLengths = new int[numDocs];
		docNames = new String[numDocs];
		
		Instance instance;
		for(int docNum = 0; docNum < numDocs; docNum++) {
			instance = training.get(docNum);
			docNames[docNum] = instance.getName().toString();
			tokens[docNum] = ((FeatureSequence) instance.getData()).getFeatures();
			docLength = tokens[docNum].length;
			docLengths[docNum] = docLength;
			
			
			topics[docNum] = new int[docLength];
			for (int position = 0; position < docLength; position++) {
				topics[docNum][position] = random.nextInt(numTopics);
			}
			
			log.print(instance.getSource());
			log.print(" ");
			
			tokenCount += docLength;
		}
		
		log.println();
		
		log.println("Loaded " + tokenCount + " tokens.");
	}

	public void sample () {
		// Compute alpha from alphaSum
		final double startingAlpha = conf.getDouble(Options.ALPHA_SUM) / (double) conf.getInt(Options.NUM_TOPICS);
		conf.putDouble(Options.ALPHA, startingAlpha);
		alphas = new double[numTopics];
		Arrays.fill(alphas, startingAlpha);
		
		// Compute betaSum from beta
		final double betaSum = conf.getDouble(Options.BETA) * (double) conf.getInt(Options.NUM_TYPES);
		conf.putDouble(Options.BETA_SUM, betaSum);
		
		final int iterations = conf.getInt(Options.ITERATIONS);
		log.println("Going to sample " + iterations + " iterations with configuration:");
		log.println(conf.toString(1));
		
		final int minThreads = Runtime.getRuntime().availableProcessors();
		final int maxThreads = Runtime.getRuntime().availableProcessors()*2;
		
		for (int iteration = 1; iteration <= iterations; iteration++) {			
			log.println("Iteration " + iteration);
			long iterationStart = System.currentTimeMillis();
			
			final double maxTopicDistance = maxTopicDistCalc.maxTopicDistance(iteration, iterations);
			
			BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
			ThreadPoolExecutor exec = new ThreadPoolExecutor(minThreads, maxThreads, 500L, TimeUnit.MILLISECONDS, queue);
			
			// Loop over every document in the corpus
			for (int docNum = 0; docNum < numDocs; docNum++) {
				exec.execute(new DocumentSampler(docNum, maxTopicDistance, topicCountsFact.create()));
			}
			
			exec.shutdown();
			
			try {
				while(!exec.awaitTermination(500L, TimeUnit.MILLISECONDS)) {
					
				}
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		
			long elapsedMillis = System.currentTimeMillis() - iterationStart;
			log.println("Iteration " + iteration + " duration: " + elapsedMillis + "ms\t");
			
			if(iteration % conf.getInt(Options.PRINT_INTERVAL) == 0) {
				log.println("Printing output");
				if(conf.isTrue(Options.PRINT_TOP_WORDS_AND_TOPICS)) {
					printTopWordsAndTopics(100, 10);
				}
				if(conf.isTrue(Options.PRINT_DOC_TOPICS)) {
					printDocumentTopics(docTopicsLog, 0.01, 100);
				}
				if(conf.isTrue(Options.PRINT_STATE)) {
					printState(stateLog);
				}
				if(conf.isTrue(Options.PRINT_LOG_LIKELIHOOD)) {
					log.println("<" + iteration + "> Log Likelihood: " + modelLogLikelihood());
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
					System.err.print(alphabet.lookupObject(typeIdx).toString() + " ");
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
				log.println();
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
		final int numTopics = conf.getInt(Options.NUM_TOPICS);
		final int numTypes = conf.getInt(Options.NUM_TYPES);
		
		double logLikelihood = 0.0;

		// Do the documents first
		int[] topicCounts = new int[numTopics];
		double[] topicLogGammas = new double[numTopics];
//		int[] docTopics;

		for (int topic=0; topic < numTopics; topic++) {
			topicLogGammas[ topic ] = Dirichlet.logGamma( alpha );
		}
	
		for (int docNum=0; docNum < numDocs; docNum++) {
//			LabelSequence topicSequence = (LabelSequence) data.get(doc).topicSequence;

//			docTopics = topicSequence.getFeatures();

			for(int topic : topics[docNum]) {
				topicCounts[topic]++;
			}
//			for (int token=0; token < docLengdocTopics.length; token++) {
//				topicCounts[ docTopics[token] ]++;
//			}

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

	private static final Comparator<Entry<Integer,Counter<Integer>>> counterCmp = new Comparator<Entry<Integer,Counter<Integer>>>(){
		@Override
		public int compare(Entry<Integer, Counter<Integer>> o1, Entry<Integer, Counter<Integer>> o2) {
			return Double.compare(o2.getValue().totalCount(), o1.getValue().totalCount());
		}
	};
	
	private static final Comparator<Entry<String,Counter<Integer>>> strCounterCmp = new Comparator<Entry<String,Counter<Integer>>>(){
		@Override
		public int compare(Entry<String, Counter<Integer>> o1, Entry<String, Counter<Integer>> o2) {
			return Double.compare(o2.getValue().totalCount(), o1.getValue().totalCount());
		}
	};
	
	private static final Comparator<Entry<Integer,Double>> countCmp = new Comparator<Entry<Integer,Double>>(){
		@Override
		public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
			return o2.getValue().compareTo(o1.getValue());
		}
	};
	
	private void printTopWordsAndTopics(int numTopics, int numWords) {
		log.print("Counting");
		CounterMap<String,Integer> docTopicCounts = new CounterMap<String,Integer>();
		CounterMap<Integer,Integer> topicWordCounts = new CounterMap<Integer,Integer>();
		
		for(int docNum = 0; docNum < numDocs; docNum++) {
			for(int i = 0; i < docLengths[docNum]; i++) {
				topicWordCounts.inc(topics[docNum][i], tokens[docNum][i]);
				docTopicCounts.inc(docNames[docNum], topics[docNum][i]);
			}
			log.print('.');
		}
		log.println();
		
		log.println("Topic words:");
		List<Entry<Integer,Counter<Integer>>> topicWordCounters = new ArrayList<Entry<Integer,Counter<Integer>>>(topicWordCounts.entrySet());
		Collections.sort(topicWordCounters, counterCmp);
		
		for(Entry<Integer,Counter<Integer>> counterEntry : topicWordCounters.subList(0, numTopics)) {
			List<Entry<Integer,Double>> countEntries = new ArrayList<Entry<Integer,Double>>(counterEntry.getValue().entries());
			Collections.sort(countEntries, countCmp);
			
			log.print("#");
			log.print(counterEntry.getKey());
			log.print(" \"");
			log.print(topicAlphabet.lookupObject(counterEntry.getKey()));
			log.print("\" [total=");
			log.print(counterEntry.getValue().totalCount());
			log.println("]:");
			log.print('\t');
			
			for(Entry<Integer,Double> countEntry : countEntries.subList(0, Math.min(countEntries.size(), numWords))) {
				Integer typeIdx = countEntry.getKey();
				Double typeCount = countEntry.getValue();
				String type = alphabet.lookupObject(typeIdx).toString();
//				log.print('\t');
				log.print(type);
				log.print("[");
				log.print(typeCount);
				log.print("] ");
			}
			log.println();
			log.println();
		}
		
		log.println("Documents topics:");
		List<Entry<String,Counter<Integer>>> docTopicCounters = new ArrayList<Entry<String,Counter<Integer>>>(docTopicCounts.entrySet());
		Collections.sort(docTopicCounters, strCounterCmp);
		
		for(Entry<String,Counter<Integer>> docTopicCounterEntry : docTopicCounters) {
			List<Entry<Integer,Double>> countEntries = new ArrayList<Entry<Integer,Double>>(docTopicCounterEntry.getValue().entries());
			Collections.sort(countEntries, countCmp);
			
			log.print(docTopicCounterEntry.getKey());
			log.print(" [total=");
			log.print(docTopicCounterEntry.getValue().totalCount());
			log.println("]:");
			
			for(Entry<Integer,Double> countEntry : countEntries.subList(0, Math.min(countEntries.size(), numWords))) {
				Integer topicIdx = countEntry.getKey();
				String topicLabel = topicAlphabet.lookupObject(topicIdx).toString();
				Double typeCount = countEntry.getValue();
				log.print("\t");
				log.print(topicLabel);
				log.print("[");
				log.print(typeCount);
				log.println("]");
			}
			log.println();
		}
		
	}


	/**
	 *  @param file        The filename to print to
	 *  @param threshold   Only print topics with proportion greater than this number
	 *  @param max         Print no more than this many topics
	 */
	private void printDocumentTopics (Log out, double threshold, int max) {
		final int numTopics = conf.getInt(Options.NUM_TOPICS);

		out.print ("#doc source topic proportion ...\n");
		int[] topicCounts = new int[ numTopics ];

		IDSorter[] sortedTopics = new IDSorter[ numTopics ];
		for (int topic = 0; topic < numTopics; topic++) {
			// Initialize the sorters with dummy values
			sortedTopics[topic] = new IDSorter(topic, topic);
		}

		if (max < 0 || max > numTopics) {
			max = numTopics;
		}

		for (int docNum = 0; docNum < numDocs; docNum++) {
			out.print (docNum); out.print (' ');
			out.print(docNames[docNum]);
			out.print (' ');

			// Count up the tokens
			for(int topic : topics[docNum]) {
				topicCounts[topic]++;
			}

			// And normalize
			for (int topic = 0; topic < numTopics; topic++) {
				sortedTopics[topic].set(topic, (double) topicCounts[topic] / (double) docLengths[docNum]);
			}
			
			Arrays.sort(sortedTopics);

			for (int i = 0; i < max; i++) {
				if (sortedTopics[i].getWeight() < threshold) { break; }
				
				out.print (sortedTopics[i].getID() + " " + 
						  sortedTopics[i].getWeight() + " ");
			}
			out.print (" \n");

			Arrays.fill(topicCounts, 0);
		}
		
	}
	
	public void printState (Log out) {
		out.println ("#doc source pos typeindex type topic");
		for (int docNum = 0; docNum < numDocs; docNum++) {
			for (int position = 0; position < docLengths[docNum]; position++) {
				out.print(docNum); out.print(' ');
				out.print(docNames[docNum]); out.print(' '); 
				out.print(position); out.print(' ');
				out.print(tokens[docNum][position]); out.print(' ');
				out.print(alphabet.lookupObject(tokens[docNum][position])); out.print(' ');
				out.print(topics[docNum][position]); out.println();
			}
		}
	}
	
}
