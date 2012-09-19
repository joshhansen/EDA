/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.	For further
information, see the file `LICENSE' included with this distribution. */
package jhn.eda;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import jhn.counts.i.i.IntIntCounter;
import jhn.counts.i.i.IntIntRAMCounter;
import jhn.eda.listeners.EDAListener;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.typetopiccounts.TopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCountsException;
import jhn.idx.Index;
import jhn.idx.RAMIndex;
import jhn.util.Config;
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
	protected transient Log log;
	public final Config conf = new Config();
	protected Randoms random;
	private List<EDAListener> listeners = new ArrayList<>();
	
	// Data sources and other helpers
	protected transient TypeTopicCounts typeTopicCounts;
	protected transient TopicCounts topicCounts;
	
	public EDA (TopicCounts topicCountsFact, TypeTopicCounts typeTopicCounts,
			final int numTopics, final String logDir) throws FileNotFoundException {
		this(topicCountsFact, typeTopicCounts, numTopics, logDir, new Randoms());
	}
	
	public EDA(TopicCounts topicCounts, TypeTopicCounts typeTopicCounts,
			final int numTopics, final String logFilename, Randoms random) throws FileNotFoundException {
		
		this.topicCounts = topicCounts;
		this.typeTopicCounts = typeTopicCounts;
		this.random = random;
		
		this.numTopics = numTopics;
		conf.putInt(Options.NUM_TOPICS, numTopics);
		
		this.logDir = logDir;
	}
	
	private void initLogging() throws FileNotFoundException {
		File logFile = new File(logDir);
		if(!logFile.exists()) {
			logFile.mkdirs();
		}
		
		log = new Log(System.out, logDir + "/main.log");
		log.println(EDA.class.getName() + ": " + numTopics + " topics");
		log.println("Topic Counts Source: " + typeTopicCounts.getClass().getSimpleName());
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

	public void sample () throws Exception {
		fireSamplerInit();
		
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
			fireIterationStarted(iteration);
			
			log.println("Iteration " + iteration);
			long iterationStart = System.currentTimeMillis();
			
			BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
			ThreadPoolExecutor exec = new ThreadPoolExecutor(minThreads, maxThreads, 500L, TimeUnit.MILLISECONDS, queue);
			
			// Loop over every document in the corpus
			for (int docNum = 0; docNum < numDocs; docNum++) {
				exec.execute(new DocumentSampler(docNum));
			}
			
			exec.shutdown();
			
			while(!exec.awaitTermination(500L, TimeUnit.MILLISECONDS)) {
				// Do nothing
			}
		
			long elapsedMillis = System.currentTimeMillis() - iterationStart;
			log.println("Iteration " + iteration + " duration: " + elapsedMillis + "ms\t");
			
			fireIterationEnded(iteration);
		}
		
		fireSamplerTerminate();
	}

	public int[] docTopicCounts(final int docNum) {
		int[] docTopicCounts = new int[numTopics];
		docTopicCounts(docNum, docTopicCounts);
		return docTopicCounts;
	}
	
	public IntIntCounter docTopicCounter(final int docNum) {
		IntIntCounter counts = new IntIntRAMCounter();
		for(int topic : topics[docNum]) {
			counts.inc(topic);
		}
		return counts;
	}
	
	private void docTopicCounts(final int docNum, final int[] topicCountsArr) {
		for(int topic : topics[docNum]) {
			topicCountsArr[topic]++;
		}
	}
	
	private class DocumentSampler implements Runnable {
		private final int docNum;
		
		public DocumentSampler(int docNum) {
			this.docNum = docNum;
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
			TopicCount ttc;
			Iterator<TopicCount> ttcIt;
			
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
						topicCount = topicCounts.topicCount(ttc.topic);
						
						countDelta = ttc.topic==oldTopic ? 1.0 : 0.0;
						score = (alphas[ttc.topic] + docTopicCounts[ttc.topic] - countDelta) *
								(beta + ttc.count) /
								(betaSum + topicCount - countDelta);
						
						sum += score;
						
						ccTopics.add(ttc.topic);
						ccScores.add(score);
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
				} catch(Exception e) {
					e.printStackTrace();
				}
			}//end for position
			
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
		int[] topicCountsArr = new int[numTopics];
		double[] topicLogGammas = new double[numTopics];

		for (int topic=0; topic < numTopics; topic++) {
			topicLogGammas[ topic ] = Dirichlet.logGamma( alpha );
		}
	
		for (int docNum=0; docNum < numDocs; docNum++) {
			for(int topic : topics[docNum]) {
				topicCountsArr[topic]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCountsArr[topic] > 0) {
					logLikelihood += (Dirichlet.logGamma(alpha + topicCountsArr[topic]) -
									  topicLogGammas[ topic ]);
				}
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGamma(alphaSum + docLengths[docNum]);

			Arrays.fill(topicCountsArr, 0);
		}
	
		// add the parameter sum term
		logLikelihood += numDocs * Dirichlet.logGamma(alphaSum);

		// Count the number of type-topic pairs
		int nonZeroTypeTopics = 0;

		for (int type=0; type < numTypes; type++) {
			try {
				Iterator<TopicCount> tcIt = typeTopicCounts.typeTopicCounts(type);
				while(tcIt.hasNext()) {
					TopicCount tc = tcIt.next();
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

	public int numDocs() {
		return numDocs;
	}
	
	public int numTopics() {
		return numTopics;
	}
	
	public int docLength(int docNum) {
		return docLengths[docNum];
	}
	
	// NOTE: gives public access to internals of private member
	public int[] docLengths() {
		return docLengths;
	}
	
	public String docName(int docNum) {
		return docNames[docNum];
	}
	
	// NOTE: gives public access to internals of private member
	public String[] docNames() {
		return docNames;
	}
	
	public int token(int docNum, int position) {
		return tokens[docNum][position];
	}
	
	public int topic(int docNum, int position) {
		return topics[docNum][position];
	}
	
	// NOTE: gives public access to internals of private member
	public int[][] topics() {
		return topics;
	}
	
	public void addListener(EDAListener l) {
		listeners.add(l);
	}
	
	public Index<String> allLabels() {
		return allLabels;
	}
	
	public String docLabel(int docNum) {
		return docLabels[docNum];
	}
	
	// NOTE: gives public access to internals of private member
	public String[] docLabels() {
		return docLabels;
	}
	
	private void fireSamplerInit() throws Exception {
		for(EDAListener l : listeners) {
			l.samplerInit(this);
		}
	}
	
	private void fireIterationStarted(int iteration) throws Exception {
		for(EDAListener sl : listeners) {
			sl.iterationStarted(iteration);
		}
	}
	
	private void fireIterationEnded(int iteration) throws Exception {
		for(EDAListener sl : listeners) {
			sl.iterationEnded(iteration);
		}
	}
	
	private void fireSamplerTerminate() throws Exception {
		for(EDAListener l : listeners) {
			l.samplerTerminate();
		}
	}
}
