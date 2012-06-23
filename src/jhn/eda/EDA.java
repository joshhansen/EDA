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
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import jhn.counts.Counter;
import jhn.counts.DoubleCounterMap;
import jhn.counts.IntIntCounter;
import jhn.counts.IntIntIntCounterMap;
import jhn.counts.ObjObjDoubleCounterMap;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.topiccounts.TopicCountsException;
import jhn.eda.topicdistance.MaxTopicDistanceCalculator;
import jhn.eda.topicdistance.StandardMaxTopicDistanceCalculator;
import jhn.eda.topicdistance.TopicDistanceCalculator;
import jhn.eda.typetopiccounts.TypeTopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCountsException;
import jhn.idx.StringIndex;
import jhn.util.Config;
import jhn.util.Factory;
import jhn.util.IntIndex;
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
	protected StringIndex allLabels;
//	// the alphabet for the input data
//	protected transient Alphabet alphabet;
//	
//	// the alphabet for the topics
//	protected transient LabelAlphabet topicAlphabet;
	
	protected final String logDir;
	protected transient Log log;
	public final Config conf = new Config();
	protected Randoms random;
	
	// Data sources and other helpers
	protected transient TypeTopicCounts typeTopicCounts;
	protected transient TopicDistanceCalculator topicDistCalc;
	protected transient MaxTopicDistanceCalculator maxTopicDistCalc = new StandardMaxTopicDistanceCalculator();
	protected transient Factory<TopicCounts> topicCountsFact;
	
	public EDA (Factory<TopicCounts> topicCountsFact, TypeTopicCounts typeTopicCounts,
			TopicDistanceCalculator topicDistCalc, String logFilename, final int numTopics) throws FileNotFoundException {
		this(topicCountsFact, typeTopicCounts, topicDistCalc, logFilename, numTopics, new Randoms());
	}
	
	public EDA(Factory<TopicCounts> topicCountsFact, TypeTopicCounts typeTopicCounts, TopicDistanceCalculator topicDistCalc,
			final String logDir, final int numTopics, Randoms random) throws FileNotFoundException {
		
		this.topicCountsFact = topicCountsFact;
		this.typeTopicCounts = typeTopicCounts;
		this.topicDistCalc = topicDistCalc;
//		this.topicAlphabet = topicAlphabet;
		this.random = random;
		
//		numTopics = topicAlphabet.size();
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
		
//		alphabet = training.getDataAlphabet();
		
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
		SortedSet<String> labels = new TreeSet<String>();
		
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
		
		allLabels = new StringIndex();
		allLabels.indexOf("none");//Needed for use in SparseInstance
		for(String theLabel : labels) {
			allLabels.indexOf(theLabel);
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
		
		final int minThreads = conf.getInt(Options.MIN_THREADS);
		final int maxThreads = conf.getInt(Options.MAX_THREADS);
		
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
						PrintStream out = new PrintStream(new FileOutputStream(logDir + "/fast_state/" + iteration + ".fast_state"));
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
						printReducedDocsLibSvm(out, conf.getInt(Options.REDUCED_DOCS_TOP_N));
						out.close();
					} catch(IOException e) {
						e.printStackTrace();
					}
					
					try {
						PrintStream out = new PrintStream(new FileOutputStream(logDir + "/reduced/" + iteration + ".libsvm_unnorm"));
						printReducedDocsLibSvm(out, conf.getInt(Options.REDUCED_DOCS_TOP_N), false);
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
			
			if(conf.containsKey(Options.ALPHA_OPTIMIZE_INTERVAL) && iteration % conf.getInt(Options.ALPHA_OPTIMIZE_INTERVAL) == 0) {
				optimizeAlphas();
			}
		}
		
		log.close();
	}
	
//	private static final double EPSILON = 0.0001;
//	private void optimizeAlphas() {
//		for(int topicNum = 0; topicNum < numTopics; topicNum++) {
//			double newAlpha;
//			double delta = Double.POSITIVE_INFINITY;
//			
//			while(delta > EPSILON) {
//				newAlpha = alphas[topicNum] - alphaOptFirstDeriv(topicNum) / alphaOptSecondDeriv(topicNum);
//				delta = Math.abs(newAlpha - alphas[topicNum]);
//				
//				// Update the alpha sum
//				alphaSum += newAlpha - alphas[topicNum];
//				
//				alphas[topicNum] = newAlpha;
//			}
//		}
//	}
//	
//	private double alphaOptFirstDeriv(final int topicNum) {
//		double sum = 0.0;
//		int[] docTopicCounts;
//		
//		for (int docNum = 0; docNum < numDocs; docNum++) {
//			docTopicCounts = docTopicCounts(docNum);
//			
//			sum += Gamma.digamma(alphas[topicNum] + docTopicCounts[topicNum])
//			       - Gamma.digamma(alphas[topicNum])
//			       + Gamma.digamma(alphaSum)
//			       - Gamma.digamma(alphaSum + (double)docLengths[docNum]);
//		}
//		
//		return sum;
//	}
//	
//	private static double alphaOptSecondDeriv(final double alpha) {
//		
//	}
	
	
	private void optimizeAlphas() {
		// Started with a literal transcription of the pseudo-code from Hanna Wallach's
		// dissertation. See Algorithm 2.2 on page 29.
		
		// Since my alpha_k is equivalent to Wallach's alpha*m_k, alpha in line 6
		// of the pseudo-code is the sum of the alphas here. Since \sum_k m_k = 1,
		// \sum_k alpha * m_k = alpha.
		System.out.println("Starting alpha optimization");

		// Determine maxes and count counts
		IntIntCounter docLengthFreqs = new IntIntCounter();
		IntIntIntCounterMap topicDocTopicFreqs = new IntIntIntCounterMap();
		int maxDocLength = 0;
//		int [] maxDocTopicCounts = new int [numTopics];
		Int2IntMap maxDocTopicCounts = new Int2IntOpenHashMap();
		int[] docTopicCounts = new int[numTopics];
		
		for(int d = 0; d < numDocs; d++) {
			docTopicCounts(d, docTopicCounts);
			
			for(int k = 0; k < numTopics; k++) {
//				if(docTopicCounts[k] > 1) {
				topicDocTopicFreqs.inc(k, docTopicCounts[k]);
//				}
				
				if(docTopicCounts[k] > maxDocTopicCounts.get(k)) {
					maxDocTopicCounts.put(k, docTopicCounts[k]);
				}
//				if(docTopicCounts[k] > maxDocTopicCounts[k]) {
//					maxDocTopicCounts[k] = docTopicCounts[k];
//				}
			}
			docLengthFreqs.inc(docLengths[d]);
			if(docLengths[d] > maxDocLength) {
				maxDocLength = docLengths[d];
			}
		}
		
 		int x = optimizeAssymetricParameters(docLengthFreqs, topicDocTopicFreqs, maxDocLength, maxDocTopicCounts);
		System.out.println(String.format("After %d rounds of iterations found new alphas:", x));
//		System.out.println(Arrays.toString(alphas));
	}

	private static final int MAX_ALPHA_OPT_ITER = 10000000;
	private static final double MIN_DIFF = 0.0000001;
	/**
	 * Uses the fixed-point iteration found in Hanna Wallach's dissertation.
	 * @param docLengthFreqs Histogram of context lengths (document lengths in the case of alphas)
	 * @param topicDocTopicCountFreqs Histogram of context/outcome counts (# occurrences of topic in documents)
	 * @param maxDocLength Maximum value in c__n
	 * @param maxDocTopicCounts.get( Maximum value in c_kn
	 * @param alphas Current value of parameters to be optimized
	 * @return
	 */
	private int optimizeAssymetricParameters(IntIntCounter docLengthFreqs, IntIntIntCounterMap topicDocTopicCountFreqs,
			int maxDocLength, Int2IntMap maxDocTopicCounts) {
		
		double diffs_sum = Double.MAX_VALUE;
		int iteration;
		for(iteration = 0; iteration < MAX_ALPHA_OPT_ITER && diffs_sum > MIN_DIFF; iteration++){
			double alphaSum = 0;
			for(int k = 0; k < alphas.length; k++) {
				alphaSum += alphas[k];
			}
			
			double D = 0;
			double S = 0;
			for(int docLength = 1; docLength <= maxDocLength; docLength++) {
				D += 1.0 / (double) (docLength-1 + alphaSum);
				if(Double.isInfinite(D)) {
					System.err.println("Danger");
				}
				final int count = docLengthFreqs.getCountI(docLength);
				S += count * D;
				if(Double.isNaN(S)) {
					System.err.println("Warning");
				}
			}
			diffs_sum = 0.0;
			for(int topicNum = 0; topicNum < alphas.length; topicNum++) {
				D = 0;
				double S_k = 0;
				for(int docTopicCount = 1; docTopicCount <= maxDocTopicCounts.get(topicNum); docTopicCount++) {
					D += 1.0 / (double) (docTopicCount - 1 + alphas[topicNum]);
					final int docTopicCountFreq = topicDocTopicCountFreqs.getCount(topicNum, docTopicCount);
					if(docTopicCountFreq > 0) {
						System.err.println("OK");
					}
					S_k += docTopicCountFreq * D;
				}
				if(S_k > 0.0) {
					System.err.println("Waddup");
				}
				if(S <= 0.0) {
					System.err.println("Pending problem");
				}
				double old_alphas_k = alphas[topicNum];
				double new_alphas_k = alphas[topicNum] * (S_k / S);
				if(new_alphas_k > 0.0) {
					System.err.println("Good");
				}
				alphas[topicNum] = new_alphas_k;
				if(Double.isNaN(alphas[topicNum])) {
					System.err.println("Trouble");
				}
				diffs_sum += Math.abs(alphas[topicNum] - old_alphas_k);
			}
		}
		return iteration;
	}

	private int[] docTopicCounts(final int docNum) {
		int[] docTopicCounts = new int[numTopics];
		docTopicCounts(docNum, docTopicCounts);
		return docTopicCounts;
	}
	
	private IntIntCounter docTopicCounter(final int docNum) {
		IntIntCounter counts = new IntIntCounter();
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

	private static final Comparator<Entry<Integer,Counter<Integer,Integer>>> counterCmp = new Comparator<Entry<Integer,Counter<Integer,Integer>>>(){
		@Override
		public int compare(Entry<Integer, Counter<Integer,Integer>> o1, Entry<Integer, Counter<Integer,Integer>> o2) {
			return o2.getValue().totalCount().compareTo(o1.getValue().totalCount());
		}
	};
	
	private static final Comparator<Entry<String,Counter<Integer,Double>>> strCounterCmp = new Comparator<Entry<String,Counter<Integer,Double>>>(){
		@Override
		public int compare(Entry<String, Counter<Integer,Double>> o1, Entry<String, Counter<Integer,Double>> o2) {
			return o2.getValue().totalCount().compareTo(o1.getValue().totalCount());
		}
	};
	
	private static final Comparator<Entry<Integer,Double>> countCmp = new Comparator<Entry<Integer,Double>>(){
		@Override
		public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
			return o2.getValue().compareTo(o1.getValue());
		}
	};
	
	private void printTopWordsAndTopics(int iteration, int numTopics, int numWords) {
		log.print("Counting");
		DoubleCounterMap<String, Integer> docTopicCounts = new ObjObjDoubleCounterMap<String,Integer>();
		IntIntIntCounterMap topicWordCounts = new IntIntIntCounterMap();
		
		for(int docNum = 0; docNum < numDocs; docNum++) {
			for(int i = 0; i < docLengths[docNum]; i++) {
				if(conf.isTrue(Options.PRINT_TOP_TOPIC_WORDS)) {
					topicWordCounts.inc(topics[docNum][i], tokens[docNum][i]);
				}
				if(conf.isTrue(Options.PRINT_TOP_DOC_TOPICS)) {
					docTopicCounts.inc(docNames[docNum], topics[docNum][i]);
				}
			}
			log.print('.');
		}
		log.println();
		
		if(conf.isTrue(Options.PRINT_TOP_TOPIC_WORDS)) {
			try {
				PrintStream out = new PrintStream(new FileOutputStream(logDir + "/top_topic_words/" + iteration + ".log"));
				printTopTopicWords(topicWordCounts, out, numTopics, numWords);
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		if(conf.isTrue(Options.PRINT_TOP_DOC_TOPICS)) {
			try {
				PrintStream out = new PrintStream(new FileOutputStream(logDir + "/top_doc_topics/" + iteration + ".log"));
				printTopDocTopics(docTopicCounts, out, numWords);
				out.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void printTopTopicWords(IntIntIntCounterMap topicWordCounts, PrintStream out, int numTopics, int numWords) {
		out.println("Topic words:");
		List<Entry<Integer,Counter<Integer,Integer>>> topicWordCounters = new ArrayList<Entry<Integer,Counter<Integer,Integer>>>(topicWordCounts.entrySet());
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
	
	private void printTopDocTopics(DoubleCounterMap<String, Integer> docTopicCounts, PrintStream out, int numWords) {
		out.println("Documents topics:");
		List<Entry<String,Counter<Integer,Double>>> docTopicCounters = new ArrayList<Entry<String,Counter<Integer,Double>>>(docTopicCounts.entrySet());
		Collections.sort(docTopicCounters, strCounterCmp);
		
		for(Entry<String,Counter<Integer,Double>> docTopicCounterEntry : docTopicCounters) {
			List<Entry<Integer,Double>> countEntries = new ArrayList<Entry<Integer,Double>>(docTopicCounterEntry.getValue().entries());
			Collections.sort(countEntries, countCmp);
			
			out.print(docTopicCounterEntry.getKey());
			out.print(" [total=");
			out.print(docTopicCounterEntry.getValue().totalCount());
			out.println("]:");
			
			for(Entry<Integer,Double> countEntry : countEntries.subList(0, Math.min(countEntries.size(), numWords))) {
				Integer topicIdx = countEntry.getKey();
				
				Double typeCount = countEntry.getValue();
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

			topicCounts = docTopicCounts(docNum);

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
	
	private void printReducedDocsLibSvm(PrintStream out, int topN) {
		printReducedDocsLibSvm(out, topN, true);
	}
	
	private void printReducedDocsLibSvm(PrintStream out, int topN, boolean normalize) {
		int classNum;
		IntIntCounter docTopicCounts;
		double docLength;
		for(int docNum = 0; docNum < numDocs; docNum++) {
			classNum = allLabels.indexOf(docLabels[docNum], false);
			docTopicCounts = docTopicCounter(docNum);
			docLength = (double) docLengths[docNum];
			
			out.print(classNum);
			
			Int2IntMap.Entry[] entries = docTopicCounts.int2IntEntrySet().toArray(new Int2IntMap.Entry[0]);
			Arrays.sort(entries, fastKeyCmp);
			
			for(Int2IntMap.Entry entry : entries) {
				out.print(' ');
				out.print(entry.getIntKey());
				out.print(':');
				if(normalize) {
					out.print( (double) entry.getIntValue() / docLength);
				} else {
					out.print(entry.getIntValue());
				}
			}
			out.println();
		}
	}
	
}
