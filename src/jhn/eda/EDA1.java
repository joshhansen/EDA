package jhn.eda;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.math3.special.Gamma;

import cc.mallet.types.Dirichlet;

import jhn.counts.i.i.IntIntCounter;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.typetopiccounts.TopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCountsException;

/** Actually LDA with static topic-word counts */
public class EDA1 extends EDA {
	private static final long serialVersionUID = 1L;
	
	protected double beta;
	protected double betaSum;
	
	public EDA1(TopicCounts topicCountsFact, TypeTopicCounts typeTopicCounts,
			int numTopics, String logDir) throws FileNotFoundException {
		super(topicCountsFact, typeTopicCounts, numTopics, logDir);
	}
	
	@Override
	public void sample() throws Exception {
		// Compute betaSum from beta
		beta = conf.getDouble(Options.BETA);
		betaSum = beta * conf.getInt(Options.NUM_TYPES);
		conf.putDouble(Options.BETA_SUM, betaSum);
		
		super.sample();
	}
	
	@Override
	protected DocumentSampler samplerInstance(int docNum) {
		return new OldEDADocumentSampler(this, docNum);
	}

	private class OldEDADocumentSampler extends DocumentSampler {
		
		public OldEDADocumentSampler(EDA eda, int docNum) {
			eda.super(docNum);
		}
		
		private double countDelta;
		private int topicCount;
		
		@Override
		protected double completeConditional(TopicCount ttc, int oldTopic, IntIntCounter docTopicCounts) throws Exception {
			topicCount = topicCorpusTopicCounts.topicCount(ttc.topic);
			
			countDelta = ttc.topic==oldTopic ? 1.0 : 0.0;
			return (alphas[ttc.topic] + docTopicCounts.getCount(ttc.topic) - countDelta) *
					(beta + ttc.count) /
					(betaSum + topicCount - countDelta);
		}
	}//end class DocumentSampler2
	
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
	@Override
	public double logLikelihood() {
		if(!usingSymmetricAlpha) {
			throw new UnsupportedOperationException("Log likelihood computation is unreasonably slow with assymetric alphas");
		}
		
		final double alpha = conf.getDouble(Options.ALPHA);
		final int numTypes = conf.getInt(Options.NUM_TYPES);
		
		double logLikelihood = 0.0;

		// Do the documents first
		int[] topicCountsArr = new int[numTopics];
//		double[] topicLogGammas = new double[numTopics];
		final double log_gamma_alpha = Gamma.logGamma(alpha);

//		for (int topic=0; topic < numTopics; topic++) {
//			topicLogGammas[ topic ] = Dirichlet.logGamma( alpha );
//		}
	
		for (int docNum=0; docNum < numDocs; docNum++) {
			for(int topic : topics[docNum]) {
				topicCountsArr[topic]++;
			}

			for (int topic=0; topic < numTopics; topic++) {
				if (topicCountsArr[topic] > 0) {
					logLikelihood += (Dirichlet.logGamma(alpha + topicCountsArr[topic]) - log_gamma_alpha);
				}
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGamma(alphaSum + docLengths[docNum]);

			Arrays.fill(topicCountsArr, 0);
		}//end for each document
	
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
		}//end for each type
	
		IntIntCounter topicCounts = targetTopicCounts();
		for (int topic=0; topic < numTopics; topic++) {
			logLikelihood -= Dirichlet.logGamma( (beta * numTopics) + topicCounts.getCount(topic));
//											tokensPerTopic[ topic ] );
			if (Double.isNaN(logLikelihood)) {
				log.println("after topic " + topic + " " + topicCounts.getCount(topic));// tokensPerTopic[ topic ]);
//				System.exit(1);
			}

		}//end for each topic
	
		logLikelihood += 
			(Dirichlet.logGamma(beta * numTopics)) -
			(Dirichlet.logGamma(beta) * nonZeroTypeTopics);

		if (Double.isNaN(logLikelihood)) {
			log.println("at the end");
			System.exit(1);
		}


		return logLikelihood;
	}
}
