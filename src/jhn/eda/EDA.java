/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.	For further
information, see the file `LICENSE' included with this distribution. */
package jhn.eda;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.Randoms;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import jhn.eda.topicdistance.MaxTopicDistanceCalculator;
import jhn.eda.topicdistance.StandardMaxTopicDistanceCalculator;
import jhn.eda.topicdistance.TopicDistanceCalculator;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCountsException;
import jhn.util.Config;
import jhn.util.Counter;
import jhn.util.CounterMap;
import jhn.util.Log;
import jhn.util.Util;

/**
* An implementation of Explicit Dirichlet Allocation using Gibbs sampling. Based on SimpleLDA by David Mimno and Andrew
* McCallum.
* 
* @author Josh Hansen
*/
public class EDA implements Serializable {
	// the training instances and their topic assignments
	protected List<TopicAssignment> data;

	// the alphabet for the input data
	protected Alphabet alphabet;
	
	// the alphabet for the topics
	protected LabelAlphabet topicAlphabet;
	
	protected final Log log;
	protected final Log docTopicsLog;
	protected final Log stateLog;
	protected Config conf = new Config();
	
	public static final double DEFAULT_ALPHA_SUM = 50.0;
	public static final double DEFAULT_BETA = 0.01;
	
	// An array to put the topic counts for the current document. 
	// Initialized locally below.  Defined here to avoid
	// garbage collection overhead.
	protected int[] oneDocTopicCounts; // indexed by <document index, topic index>

	// Statistics needed for sampling.
	protected int[] tokensPerTopic; // indexed by <topic index>

	protected synchronized void incTokensPerTopic(int topic) {
		tokensPerTopic[topic]++;
	}
	
	protected synchronized void decTokensPerTopic(int topic) {
		tokensPerTopic[topic]--;
	}
	
	protected Randoms random;
	protected TypeTopicCounts typeTopicCounts;
	protected TopicDistanceCalculator topicDistCalc;
	protected MaxTopicDistanceCalculator maxTopicDistCalc = new StandardMaxTopicDistanceCalculator();
	
	public EDA(TypeTopicCounts typeTopicCounts, TopicDistanceCalculator topicDistCalc, String logFilename, LabelAlphabet topicAlphabet) throws FileNotFoundException {
		this(typeTopicCounts, topicDistCalc, logFilename, topicAlphabet, DEFAULT_ALPHA_SUM, DEFAULT_BETA);
	}
	
	public EDA (TypeTopicCounts typeTopicCounts, TopicDistanceCalculator topicDistCalc, String logFilename, LabelAlphabet topicAlphabet, double alphaSum, double beta) throws FileNotFoundException {
		this(typeTopicCounts, topicDistCalc, logFilename, topicAlphabet, alphaSum, beta, new Randoms());
	}
	
	public EDA(TypeTopicCounts typeTopicCounts, TopicDistanceCalculator topicDistCalc, final String logFilename, final LabelAlphabet topicAlphabet, double alphaSum, double beta, Randoms random) throws FileNotFoundException {
		this.typeTopicCounts = typeTopicCounts;
		this.topicDistCalc = topicDistCalc;
		
		this.data = new ArrayList<TopicAssignment>();
		this.topicAlphabet = topicAlphabet;
		final int numTopics = topicAlphabet.size();

		conf.putDouble(Options.ALPHA_SUM, alphaSum);
		conf.putDouble(Options.ALPHA, alphaSum / numTopics);
		conf.putDouble(Options.BETA, beta);
		conf.putInt(Options.NUM_TOPICS, numTopics);
		
		this.random = random;
		
		oneDocTopicCounts = new int[numTopics];
		tokensPerTopic = new int[numTopics];
		
		// Start logging
		log = new Log(System.out, logFilename);
		log.println(EDA.class.getName() + ": " + numTopics + " topics");
		log.println("Topic Counts Source: " + typeTopicCounts.getClass().getSimpleName());
		log.println("Topic Distance Calc: " + topicDistCalc.getClass().getSimpleName());
		log.println("Max Topic Distance Calc: " + maxTopicDistCalc.getClass().getSimpleName());
		docTopicsLog = new Log(logFilename+".doctopics");
		stateLog = new Log(logFilename+".state");
	}
	
	public Config config() {
		return conf;
	}

	public void addInstances (InstanceList training) {
		final int numTopics = conf.getInt(Options.NUM_TOPICS);
		
		log.println("Dataset instances: " + training.size());
		
		alphabet = training.getDataAlphabet();
		
		//FIXME This keeps overwriting NUM_TYPES and BETA_SUM every time an instance is added.
		final int numTypes = alphabet.size();
		conf.putInt(Options.NUM_TYPES, numTypes);
		conf.putDouble(Options.BETA_SUM, conf.getDouble(Options.BETA) * numTypes);

		int doc = 0;

		FeatureSequence tokens;
		LabelSequence topicSequence;
		
		int tokenCount = 0;
		
		log.print("Loading: ");
		for (Instance instance : training) {
			doc++;

			tokens = (FeatureSequence) instance.getData();
			topicSequence = new LabelSequence(topicAlphabet, new int[ tokens.size() ]);
			
			int[] topics = topicSequence.getFeatures();
			for (int position = 0; position < tokens.size(); position++) {
				int topic = random.nextInt(numTopics);
				topics[position] = topic;
				tokensPerTopic[topic]++;
			}
			
			log.print(instance.getSource());
			log.print(" ");
			
			data.add (new TopicAssignment (instance, topicSequence));
			
			tokenCount += tokens.getLength();
		}
		
		log.println();
		
		log.println("Loaded " + tokenCount + " tokens.");
	}

	public void sample (int iterations) {
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
			for (int doc = 0; doc < data.size(); doc++) {
				exec.execute(new DocumentSampler(doc, maxTopicDistance));
			}
			
			exec.shutdown();
			
			try {
				while(!exec.awaitTermination(500L, TimeUnit.MILLISECONDS)) {
					
				}
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
			log.println();
		
			long elapsedMillis = System.currentTimeMillis() - iterationStart;
			log.println(iteration + "\t" + elapsedMillis + "ms\t");
			
			
			if(conf.containsKey(Options.PRINT_TOP_WORDS_AND_TOPICS) && conf.getBool(Options.PRINT_TOP_WORDS_AND_TOPICS)) {
				printTopWordsAndTopics(100, 10);
			}
			if(conf.containsKey(Options.PRINT_DOC_TOPICS) && conf.getBool(Options.PRINT_DOC_TOPICS)) {
				printDocumentTopics(docTopicsLog, 0.01, 100);
			}
			if(conf.containsKey(Options.PRINT_STATE) && conf.getBool(Options.PRINT_STATE)) {
				printState(stateLog);
			}
			
			// Occasionally print more information
			int showTopicsInterval = conf.getInt(Options.SHOW_TOPICS_INTERVAL);
			if (/*showTopicsInterval != 0 &&*/ iteration % showTopicsInterval == 0) {
//				logger.info("<" + iteration + "> Log Likelihood: " + modelLogLikelihood() + "\n" +
//							topWords (wordsPerTopic));
				
				
				
				if(conf.containsKey(Options.PRINT_LOG_LIKELIHOOD) && conf.getBool(Options.PRINT_LOG_LIKELIHOOD)) {
					log.println("<" + iteration + "> Log Likelihood: " + modelLogLikelihood());
				}
//				log.print("<" + iteration + ">\n" + topTopics(100));
			}
			log.println();
		}
		
		log.close();
	}
	
	// Topic and type filters
	private static final Pattern months = Pattern.compile("january|february|march|april|may|june|july|august|september|october|november|december");
	private static final Pattern digits = Pattern.compile("\\d+");
	private Set<String> preselectedFeatures = null;
	
	@SuppressWarnings("unchecked")
	public boolean shouldFilterType(int typeIdx) {
		String type = alphabet.lookupObject(typeIdx).toString();
		
		if(conf.containsKey(Options.PRESELECTED_FEATURES)) {
			if(preselectedFeatures==null) {
				preselectedFeatures = (Set<String>) conf.getObj(Options.PRESELECTED_FEATURES);
			}
			if(!preselectedFeatures.contains(type)) return true;
		}
		
		if(conf.containsKey(Options.FILTER_DIGITS) && conf.getBool(Options.FILTER_DIGITS) && digits.matcher(type).matches()) return true;
		
		if(conf.containsKey(Options.FILTER_MONTHS) && conf.getBool(Options.FILTER_MONTHS) && months.matcher(type).matches()) return true;
		
		return false;
	}
	
	public boolean shouldFilterTopic(TopicCount tc) {
		if(conf.containsKey(Options.TYPE_TOPIC_MIN_COUNT) && tc.count < conf.getInt(Options.TYPE_TOPIC_MIN_COUNT)) return true;
		return false;
	}
	
	private class DocumentSampler implements Runnable {
		private final int docNum;
		private final double maxTopicDistance;
		public DocumentSampler(int docNum, double maxTopicDistance) {
			this.docNum = docNum;
			this.maxTopicDistance = maxTopicDistance;
		}
		
		@Override
		public void run() {
			final double alpha = conf.getDouble(Options.ALPHA);
			final double beta = conf.getDouble(Options.BETA);
			final double betaSum = conf.getDouble(Options.BETA_SUM);
			final int numTopics = conf.getInt(Options.NUM_TOPICS);
			
			final TopicAssignment ta = data.get(docNum);
			final FeatureSequence tokenSequence = (FeatureSequence) ta.instance.getData();
			final LabelSequence topicSequence = (LabelSequence) ta.topicSequence;

			int[] oneDocTopics = topicSequence.getFeatures();
			
			int typeIdx, oldTopic, newTopic;
			int docLength = tokenSequence.getLength();

			int[] localTopicCounts = new int[numTopics];

			// populate topic counts
			for (int position = 0; position < docLength; position++) {
				localTopicCounts[oneDocTopics[position]]++;
			}
			
			IntList topics;
			DoubleList scores;
			
			int i;
			double score, sum, sample;
			TopicCount tc;
			Iterator<TopicCount> tcIt;
			
			//	Iterate over the positions (words) in the document 
			for (int position = 0; position < docLength; position++) {
				typeIdx = tokenSequence.getIndexAtPosition(position);
				if(!shouldFilterType(typeIdx)) {
					topics = new IntArrayList();
					scores = new DoubleArrayList();
					try {
						oldTopic = oneDocTopics[position];
			
						//	Remove this token from all counts. 
						localTopicCounts[oldTopic]--;
						decTokensPerTopic(oldTopic);
						assert(tokensPerTopic[oldTopic] >= 0) : "old Topic " + oldTopic + " below 0";
			
						// Now calculate and add up the scores for each topic for this word
						sum = 0.0;
			
						// Here's where the math happens! Note that overall performance is 
						//  dominated by what you do in this loop.
						tcIt = typeTopicCounts.typeTopicCounts(typeIdx);
						while(tcIt.hasNext()) {
							tc = tcIt.next();
							
							boolean topicInRange = topicDistCalc.topicDistance(oldTopic, tc.topic) <= maxTopicDistance;
							
							if(topicInRange) {
								if(!shouldFilterTopic(tc)) {
									score =
										(alpha + localTopicCounts[tc.topic]) *
										((beta + tc.count - (tc.topic==oldTopic ? 1 : 0)) /
										 (betaSum + tokensPerTopic[tc.topic]));
									sum += score;
									
									topics.add(tc.topic);
									scores.add(score);
								}
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
								newTopic = topics.getInt(i);
								sample -= scores.getDouble(i);
							}
				
							// Make sure we actually sampled a topic
							if (newTopic == -1) {
								throw new IllegalStateException (EDA.class.getName()+": New topic not sampled.");
							}
			
							// Put that new topic into the counts
							oneDocTopics[position] = newTopic;
							localTopicCounts[newTopic]++;
							incTokensPerTopic(newTopic);
						}
					} catch(TypeTopicCountsException e) {
						// Words that occur in none of the topics will lead us here
						System.err.print(alphabet.lookupObject(typeIdx).toString() + " ");
					}
				}//end if should filter type
			}//end for
			log.print('.');
		}
	}
	

	
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
		int[] docTopics;

		for (int topic=0; topic < numTopics; topic++) {
			topicLogGammas[ topic ] = Dirichlet.logGamma( alpha );
		}
	
		for (int doc=0; doc < data.size(); doc++) {
			LabelSequence topicSequence = (LabelSequence) data.get(doc).topicSequence;

			docTopics = topicSequence.getFeatures();

			for (int token=0; token < docTopics.length; token++) {
				topicCounts[ docTopics[token] ]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCounts[topic] > 0) {
					logLikelihood += (Dirichlet.logGamma(alpha + topicCounts[topic]) -
									  topicLogGammas[ topic ]);
				}
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGamma(alphaSum + docTopics.length);

			Arrays.fill(topicCounts, 0);
		}
	
		// add the parameter sum term
		logLikelihood += data.size() * Dirichlet.logGamma(alphaSum);

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
	
		for (int topic=0; topic < numTopics; topic++) {
			logLikelihood -= 
				Dirichlet.logGamma( (beta * numTopics) +
											tokensPerTopic[ topic ] );
			if (Double.isNaN(logLikelihood)) {
				log.println("after topic " + topic + " " + tokensPerTopic[ topic ]);
				System.exit(1);
			}

		}
	
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
		
		for(TopicAssignment ta : data) {
			String filename = ta.instance.getName().toString();
			
			
			final FeatureSequence tokenSequence = (FeatureSequence) ta.instance.getData();
			final LabelSequence topicSequence = (LabelSequence) ta.topicSequence;

			int[] tokens = tokenSequence.getFeatures();
			int[] topics = topicSequence.getFeatures();
			
			for(int i = 0; i < tokens.length; i++) {
				topicWordCounts.inc(topics[i], tokens[i]);
				docTopicCounts.inc(filename, topics[i]);
			}
			System.out.print('.');
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
		int docLen;
		int[] topicCounts = new int[ numTopics ];

		IDSorter[] sortedTopics = new IDSorter[ numTopics ];
		for (int topic = 0; topic < numTopics; topic++) {
			// Initialize the sorters with dummy values
			sortedTopics[topic] = new IDSorter(topic, topic);
		}

		if (max < 0 || max > numTopics) {
			max = numTopics;
		}

		for (int doc = 0; doc < data.size(); doc++) {
			LabelSequence topicSequence = (LabelSequence) data.get(doc).topicSequence;
			int[] currentDocTopics = topicSequence.getFeatures();

			out.print (doc); out.print (' ');

			if (data.get(doc).instance.getSource() != null) {
				out.print (data.get(doc).instance.getSource()); 
			}
			else {
				out.print ("null-source");
			}

			out.print (' ');
			docLen = currentDocTopics.length;

			// Count up the tokens
			for (int token=0; token < docLen; token++) {
				topicCounts[ currentDocTopics[token] ]++;
			}

			// And normalize
			for (int topic = 0; topic < numTopics; topic++) {
				sortedTopics[topic].set(topic, (float) topicCounts[topic] / docLen);
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

		for (int doc = 0; doc < data.size(); doc++) {
			FeatureSequence tokenSequence =	(FeatureSequence) data.get(doc).instance.getData();
			LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;

			String source = "NA";
			if (data.get(doc).instance.getSource() != null) {
				source = data.get(doc).instance.getSource().toString();
			}

			for (int position = 0; position < topicSequence.getLength(); position++) {
				int type = tokenSequence.getIndexAtPosition(position);
				int topic = topicSequence.getIndexAtPosition(position);
				out.print(doc); out.print(' ');
				out.print(source); out.print(' '); 
				out.print(position); out.print(' ');
				out.print(type); out.print(' ');
				out.print(alphabet.lookupObject(type)); out.print(' ');
				out.print(topic); out.println();
			}
		}
	}
	
	
	// Serialization
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	
	public void write (File f) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream(f));
			oos.writeObject(this);
			oos.close();
		} catch (IOException e) {
			System.err.println("Exception writing file " + f + ": " + e);
		}
	}
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);

		// Config
		out.writeObject(conf);
		
		// Instance lists
		out.writeObject (data);
		out.writeObject (alphabet);
		out.writeObject (topicAlphabet);

		out.writeObject(random);

		for (int ti = 0; ti < conf.getInt(Options.NUM_TOPICS); ti++) {
			out.writeInt (tokensPerTopic[ti]);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		@SuppressWarnings("unused")
		int version = in.readInt ();

		conf = (Config) in.readObject();
		
		data = (ArrayList<TopicAssignment>) in.readObject ();
		alphabet = (Alphabet) in.readObject();
		topicAlphabet = (LabelAlphabet) in.readObject();

		random = (Randoms) in.readObject();

		final int numTopics = conf.getInt(Options.NUM_TOPICS);
		tokensPerTopic = new int[numTopics];
		for (int ti = 0; ti < numTopics; ti++) {
			tokensPerTopic[ti] = in.readInt();
		}
	}
}
