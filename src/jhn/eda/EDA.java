/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.	For further
information, see the file `LICENSE' included with this distribution. */
package jhn.eda;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
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

/**
* An implementation of Explicit Dirichlet Allocation using Gibbs sampling. Based on SimpleLDA by David Mimno and Andrew
* McCallum.
* 
* @author Josh Hansen
* @author David Mimno
* @author Andrew McCallum
*/
public abstract class EDA implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected final int numTopics;
	protected int numDocs;
	protected int[][] tokens;
	protected int[][] topics;
	protected int[] docLengths;
	protected int[] docLengthCounts;
	protected int maxDocLength;
	
	protected String[] docNames;
	protected double[] alphas;
	protected double alphaSum;
	protected int alphaOptimizeInterval;
	
	
	// Classification helpers
	protected String[] docLabels;
	protected Index<String> allLabels;
	
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
		
		initLogging(logFilename);
	}
	
	private void initLogging(String logFilename) throws FileNotFoundException {
		log = new Log(System.out, logFilename);
		log.println(getClass().getName() + ": " + numTopics + " topics");
		log.println("Topic Counts Source: " + typeTopicCounts.getClass().getSimpleName());
	}

	public void setTrainingData (InstanceList training) {
		numDocs = training.size();
		
		log.println("Dataset instances: " + training.size());
		
		final int numTypes = training.getDataAlphabet().size();
		conf.putInt(Options.NUM_TYPES, numTypes);
		
		conf.putClass("CLASS", getClass());
		
		log.print("Loading: ");
		int tokenCount = 0;
		int docLength;
		maxDocLength = 0;
		tokens = new int[numDocs][];
		topics = new int[numDocs][];
		docLengths = new int[numDocs];
		docNames = new String[numDocs];
		
		docLabels = new String[numDocs];
		SortedSet<String> labels = new TreeSet<>();
		
		Instance instance;
		String label;
		IntIntCounter docLengthFreqs = new IntIntRAMCounter();
		for(int docNum = 0; docNum < numDocs; docNum++) {
			instance = training.get(docNum);
			docNames[docNum] = instance.getName().toString();
			tokens[docNum] = ((FeatureSequence) instance.getData()).getFeatures();
			docLength = tokens[docNum].length;
			docLengths[docNum] = docLength;
			docLengthFreqs.inc(docLength);
			if(docLength > maxDocLength) {
				maxDocLength = docLength;
			}
			
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
		// Alpha optimization stuff:
		docLengthCounts = new int[maxDocLength];
		for(int i = 0; i < docLengthCounts.length; i++) {
			docLengthCounts[i] = docLengthFreqs.getCount(i);
		}
		topicDocCounts = new int[numTopics][maxDocLength+1];
		
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
		alphaSum = conf.getDouble(Options.ALPHA_SUM);
		final double startingAlpha = alphaSum / conf.getInt(Options.NUM_TOPICS);
		conf.putDouble(Options.ALPHA, startingAlpha);
		alphas = new double[numTopics];
		Arrays.fill(alphas, startingAlpha);
		
		alphaOptimizeInterval = conf.getInt(Options.ALPHA_OPTIMIZE_INTERVAL);
		
		final int iterations = conf.getInt(Options.ITERATIONS);
		log.println("Going to sample " + iterations + " iterations with configuration:");
		log.println(conf.toString(1));
		
		final int minThreads = conf.getInt(Options.MIN_THREADS);
		final int maxThreads = conf.getInt(Options.MAX_THREADS);
		
		
		for (int iteration = 1; iteration <= iterations; iteration++) {		
			fireIterationStarted(iteration);
			
			log.println("Iteration " + iteration);
			long iterationStart = System.currentTimeMillis();
			
			clearAlphaOptimizationHistogram();
			
			ThreadPoolExecutor exec = new ThreadPoolExecutor(minThreads, maxThreads, 500L, TimeUnit.MILLISECONDS,
																new LinkedBlockingQueue<Runnable>());
			
			// Loop over every document in the corpus
			for (int docNum = 0; docNum < numDocs; docNum++) {
				exec.execute(samplerInstance(docNum));
			}
			
			exec.shutdown();
			
			while(!exec.awaitTermination(500L, TimeUnit.MILLISECONDS)) {
				// Do nothing
			}
			
			if(iteration % alphaOptimizeInterval == 0) {
				System.out.print("Optimizing alphas...");
				
				optimizeAlphas();
				
				System.out.println("done.");
			}
		
			long elapsedMillis = System.currentTimeMillis() - iterationStart;
			log.println("Iteration " + iteration + " duration: " + elapsedMillis + "ms\t");
			
			fireIterationEnded(iteration);
		}
		
		fireSamplerTerminate();
	}
	
	protected abstract DocumentSampler samplerInstance(int docNum);

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
	
	protected abstract class DocumentSampler implements Runnable {
		protected final int docNum;
		
		public DocumentSampler(int docNum) {
			this.docNum = docNum;
		}
		
		protected abstract double completeConditional(TopicCount ttc, int oldTopic, int[] docTopicCounts) throws Exception;
		
		@Override
		public void run() {
			int typeIdx, oldTopic, newTopic;
			int docLength = docLengths[docNum];

			int[] docTopicCounts = docTopicCounts(this.docNum);
			
			IntList ccTopics = new IntArrayList();
			DoubleList ccScores = new DoubleArrayList();
			
			int i;
			double score, sum, sample;
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
						
						score = completeConditional(ttc, oldTopic, docTopicCounts);
						
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
			
			// Update topicDocCounts
			System.out.print("Updating topicDocCounts...");
			synchronized(topicDocCounts) {
				for(int topic = 0; topic < numTopics; topic++) {
					topicDocCounts[topic][docTopicCounts[topic]] += 1;
				}
			}
			System.out.println("done.");
			
			samplerFinished();
		}
		
		private void samplerFinished() {
			log.print('.');
			if(docNum > 0 && docNum % 120 == 0) {
				log.println(docNum);
			}
		}
	}//end class DocumentSampler
	
	//BEGIN from ParallelTopicModel
	// for dirichlet estimation
//	public int[] docLengthCounts; // histogram of document sizes
	
	public int[][] topicDocCounts; // histogram of document/topic counts, indexed by <topic index, sequence position index>
	
	private void clearAlphaOptimizationHistogram() {
//		Arrays.fill(docLengthCounts, 0);
		for (int topic = 0; topic < topicDocCounts.length; topic++) {
			Arrays.fill(topicDocCounts[topic], 0);
		}
	}
	
	private static final boolean usingSymmetricAlpha = false;
	public void optimizeAlphas() {
//		for (int thread = 0; thread < numThreads; thread++) {
//			int[][] sourceTopicCounts = runnables[thread].getTopicDocCounts();
//
//			for (int topic=0; topic < numTopics; topic++) {
//
//				if (! usingSymmetricAlpha) {
//					for (int count=0; count < sourceTopicCounts[topic].length; count++) {
//						if (sourceTopicCounts[topic][count] > 0) {
//							topicDocCounts[topic][count] += sourceTopicCounts[topic][count];
//							sourceTopicCounts[topic][count] = 0;
//						}
//					}
//				}
//				else {
//					// For the symmetric version, we only need one 
//					//  count array, which I'm putting in the same 
//					//  data structure, but for topic 0. All other
//					//  topic histograms will be empty.
//					// I'm duplicating this for loop, which 
//					//  isn't the best thing, but it means only checking
//					//  whether we are symmetric or not numTopics times, 
//					//  instead of numTopics * longest document length.
//					for (int count=0; count < sourceTopicCounts[topic].length; count++) {
//						if (sourceTopicCounts[topic][count] > 0) {
//							topicDocCounts[0][count] += sourceTopicCounts[topic][count];
//							//			 ^ the only change
//							sourceTopicCounts[topic][count] = 0;
//						}
//					}
//				}
//			}
//		}

		if (usingSymmetricAlpha) {
			alphaSum = Dirichlet.learnSymmetricConcentration(topicDocCounts[0], docLengthCounts, numTopics, alphaSum);
			for (int topic = 0; topic < numTopics; topic++) {
				alphas[topic] = alphaSum / numTopics;
			}
		} else {
			alphaSum = Dirichlet.learnParameters(alphas, topicDocCounts, docLengthCounts, 1.001, 1.0, 1);
		}
	}
	// END from ParallelTopicModel

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
