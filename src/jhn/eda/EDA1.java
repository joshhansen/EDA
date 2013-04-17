package jhn.eda;

import java.io.FileNotFoundException;

import jhn.counts.i.i.IntIntCounter;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.typetopiccounts.TopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;

/** Actually LDA with static topic-word counts */
public class EDA1 extends EDA {
	public EDA1(TopicCounts topicCountsFact, TypeTopicCounts typeTopicCounts,
			int numTopics, String logDir) throws FileNotFoundException {
		super(topicCountsFact, typeTopicCounts, numTopics, logDir);
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 1L;
	
	protected double beta;
	protected double betaSum;
	
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
			topicCount = topicCounts.topicCount(ttc.topic);
			
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
		throw new UnsupportedOperationException();
	}
}
