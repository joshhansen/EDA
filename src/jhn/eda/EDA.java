/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.	For further
information, see the file `LICENSE' included with this distribution. */
package jhn.eda;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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

import jhn.util.Config;
import jhn.util.Util;

/**
* An implementation of Explicit Dirichlet Allocation using Gibbs sampling. Based on SimpleLDA by David Mimno and Andrew
* McCallum.
* 
* @author Josh Hansen
*/
public abstract class EDA implements Serializable {
	// the training instances and their topic assignments
	protected List<TopicAssignment> data;

	// the alphabet for the input data
	protected Alphabet alphabet;
	
	// the alphabet for the topics
	protected LabelAlphabet topicAlphabet;
	
	// The number of topics requested
	protected int numTopics;

	// The size of the vocabulary
	protected int numTypes;
	
	protected final Log log;
	protected Config conf = new Config();

	// Prior parameters
	protected double alpha; // Dirichlet(alpha,alpha,...) is the distribution over topics
	protected double alphaSum;
	protected double beta; // Prior on per-topic multinomial distribution over words
	protected double betaSum;
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
	protected boolean printLogLikelihood = false;
	
	public EDA (final String logFilename, final LabelAlphabet topicAlphabet, double alphaSum, double beta) {
		this(logFilename, topicAlphabet, alphaSum, beta, new Randoms());
	}
	
	public EDA(final String logFilename, final LabelAlphabet topicAlphabet, double alphaSum, double beta, Randoms random) {
		this.data = new ArrayList<TopicAssignment>();
		this.topicAlphabet = topicAlphabet;
		this.numTopics = topicAlphabet.size();

		this.alphaSum = alphaSum;
		this.alpha = alphaSum / numTopics;
		this.beta = beta;
		this.random = random;
		
		oneDocTopicCounts = new int[numTopics];
		tokensPerTopic = new int[numTopics];
		
		log = new Log(logFilename);

		log.println(EDA.class.getName() + ": " + numTopics + " topics");
	}
	
	public Config config() {
		return conf;
	}
	
	protected abstract Iterator<TopicCount> typeTopicCounts(int typeIdx);

	public void addInstances (InstanceList training) {
		log.println("Dataset instances: " + training.size());
		
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		
		betaSum = beta * numTypes;

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
		log.println(conf.toString());
		
		final int minThreads = Runtime.getRuntime().availableProcessors();
		final int maxThreads = Runtime.getRuntime().availableProcessors()*2;
		
		for (int iteration = 1; iteration <= iterations; iteration++) {
			log.println("Iteration " + iteration);
			long iterationStart = System.currentTimeMillis();
			
			BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
			ThreadPoolExecutor exec = new ThreadPoolExecutor(minThreads, maxThreads, 500L, TimeUnit.MILLISECONDS, queue);
			
			// Loop over every document in the corpus
			for (int doc = 0; doc < data.size(); doc++) {
				exec.execute(new DocumentSampler(doc));
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
			
			// Occasionally print more information
			int showTopicsInterval = conf.getInt(Options.SHOW_TOPICS_INTERVAL);
			if (showTopicsInterval != 0 && iteration % showTopicsInterval == 0) {
//				logger.info("<" + iteration + "> Log Likelihood: " + modelLogLikelihood() + "\n" +
//							topWords (wordsPerTopic));
				if(printLogLikelihood) {
					log.println("<" + iteration + "> Log Likelihood: " + modelLogLikelihood());
				}
				log.print("<" + iteration + ">\n" + topTopics(100));
			}
			log.println();
		}
		
		log.close();
	}
	
	// Topic and type filters
	private static final Pattern months = Pattern.compile("january|february|march|april|may|june|july|august|september|october|november|december");
	private static final Pattern digits = Pattern.compile("\\d+");
	public boolean shouldFilterType(int typeIdx) {
		String type = alphabet.lookupObject(typeIdx).toString();
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
		public DocumentSampler(int docNum) {
			this.docNum = docNum;
		}
		
		@Override
		public void run() {
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
						tcIt = typeTopicCounts(typeIdx);
						while(tcIt.hasNext()) {
							tc = tcIt.next();
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
					} catch(IllegalArgumentException e) {
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
				Iterator<TopicCount> tcIt = typeTopicCounts(type);
				while(tcIt.hasNext()) {
					TopicCount tc = tcIt.next();
					nonZeroTypeTopics++;
					logLikelihood += Dirichlet.logGamma(beta + tc.count);
					if (Double.isNaN(logLikelihood)) {
						log.println(tc.count);
						System.exit(1);
					}
				}
			} catch(IllegalArgumentException e) {
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

//	public String topWords (int numWords) {
//
//		StringBuilder output = new StringBuilder();
//
//		IDSorter[] sortedWords = new IDSorter[numTypes];
//
//		for (int topic = 0; topic < numTopics; topic++) {
//			for (int type = 0; type < numTypes; type++) {
//				
//				sortedWords[type] = new IDSorter(type, typeTopicCount(type, topic));
//			}
//
//			Arrays.sort(sortedWords);
//			
//			output.append(topic + "\t" + tokensPerTopic[topic] + "\t");
//			for (int i=0; i < numWords; i++) {
//				output.append(alphabet.lookupObject(sortedWords[i].getID()) + " ");
//			}
//			output.append("\n");
//		}
//
//		return output.toString();
//	}
	
	public String topTopics(int numTopics) {
		TopNTracker topNtracker = new TopNTracker(numTopics);
		for(int i = 0; i < tokensPerTopic.length; i++) {
			topNtracker.add(tokensPerTopic[i], i);
		}
		List<Value> topN = topNtracker.topN();
		
		StringBuilder output = new StringBuilder();
		output.append("Top ").append(numTopics).append(" topics:\n");
		for(Value v : topN) {
			output.append("\t#").append(v.position).append(" \"").append(topicAlphabet.lookupObject(v.position)).append("\": ")
			.append(v.value).append('\n');
		}
		return output.toString();
	}

	/**
	 *  @param file        The filename to print to
	 *  @param threshold   Only print topics with proportion greater than this number
	 *  @param max         Print no more than this many topics
	 */
	public void printDocumentTopics (File file, double threshold, int max) throws IOException {
		PrintWriter out = new PrintWriter(file);

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
	
	public void printState (File f) throws IOException {
		PrintStream out = new PrintStream(Util.smartOutputStream(f.getParent(), true));
		printState(out);
		out.close();
	}
	
	public void printState (PrintStream out) {

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

		out.writeInt (numTopics);
		out.writeDouble (alpha);
		out.writeDouble (beta);
		out.writeDouble (betaSum);

		out.writeObject(random);
		out.writeBoolean(printLogLikelihood);

		for (int ti = 0; ti < numTopics; ti++) {
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

		numTopics = in.readInt();
		alpha = in.readDouble();
		alphaSum = alpha * numTopics;
		beta = in.readDouble();
		betaSum = in.readDouble();

		random = (Randoms) in.readObject();
		printLogLikelihood = in.readBoolean();
		
		numTypes = alphabet.size();

		tokensPerTopic = new int[numTopics];
		for (int ti = 0; ti < numTopics; ti++) {
			tokensPerTopic[ti] = in.readInt();
		}
	}
}
