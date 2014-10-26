/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.	For further
information, see the file `LICENSE' included with this distribution. */
package jhn.eda;

import java.io.FileNotFoundException;
import java.util.Iterator;

import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.util.FastMath;

import it.unimi.dsi.fastutil.ints.Int2IntMap;

import jhn.counts.i.i.IntIntCounter;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.typetopiccounts.TopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCountsException;

/**
* An implementation of Explicit Dirichlet Allocation using Gibbs sampling. Based on SimpleLDA by David Mimno and Andrew
* McCallum.
* 
* @author Josh Hansen
* @author David Mimno
* @author Andrew McCallum
*/
public class EDA extends ProbabilisticExplicitTopicModel {
	private static final long serialVersionUID = 1L;
	
	public EDA(TopicCounts topicCountsFact, TypeTopicCounts typeTopicCounts,
			int numTopics, String logDir) throws FileNotFoundException {
		super(topicCountsFact, typeTopicCounts, numTopics, logDir);
	}
	
	@Override
	protected DocumentSampler samplerInstance(int docNum) {
		return new DocumentSampler2(this, docNum);
	}

	private class DocumentSampler2 extends DocumentSampler {
		
		public DocumentSampler2(ProbabilisticExplicitTopicModel eda, int docNum) {
			eda.super(docNum);
		}
		
		private double topicCount;
		private double topicWordProb;
		private double docTopicCount;
		private double typeTopicCount;
		private double score;
		
		
		@Override
		protected double completeConditional(TopicCount ttc, int oldTopic, IntIntCounter docTopicCounts) throws Exception {
			typeTopicCount = ttc.count;
			topicCount = topicCorpusTopicCounts.topicCount(ttc.topic);
			topicWordProb = topicCount != 0 ? typeTopicCount / topicCount : 0.0;
			docTopicCount = docTopicCounts.getCount(ttc.topic) - (oldTopic==ttc.topic ? 1.0 : 0.0);
			score = topicWordProb*(docTopicCount + alphas[ttc.topic]);
			if(Double.isInfinite(score)) {
				System.err.println("Problem");
			}
			return score;
		}
	}//end class DocumentSampler2

	@Override
	public double logLikelihood() throws Exception {
		if(!usingSymmetricAlpha) throw new UnsupportedOperationException("Log likelihood computation is unreasonably slow with assymetric alphas");
		
		//NOTE: assumes symmetric alphas!
		final double alpha = alphas[0];
		final double Kalpha = numTopics * alpha;
		final double log_gamma_alpha = Gamma.logGamma(alpha);
		
		
		double ll = Gamma.logGamma(Kalpha);
		ll -= numTopics * log_gamma_alpha;
		
		int nonZeroTopics;
		int zeroTopics;
		
		IntIntCounter docTopicCounts;
		for(int docNum = 0; docNum < numDocs; docNum++) {
			docTopicCounts = docTopicCounts(docNum);
			
			// Deal with non-zero-count topics:
			for(Int2IntMap.Entry entry : docTopicCounts.int2IntEntrySet()) {
				ll += Gamma.logGamma(entry.getIntValue() + alpha);
				
			}
			
			// Deal with zero-count topics:
			nonZeroTopics = docTopicCounts.size();
			zeroTopics = numTopics - nonZeroTopics;
			ll += zeroTopics * log_gamma_alpha;
			
			
			ll -= Gamma.logGamma(tokens[docNum].length + Kalpha);
			
			for(int position = 0; position < docLengths[docNum]; position++) {
				double topicWordProb = Double.NaN;
				Iterator<TopicCount> it = typeTopicCounts.typeTopicCounts(tokens[docNum][position]);
				while(it.hasNext()) {
					TopicCount tc = it.next();
					if(tc.topic==topics[docNum][position]) {
						topicWordProb = (double)tc.count / (double) topicCorpusTopicCounts.topicCount(tc.topic);
						break;
					}
				}
				
				if(Double.isNaN(topicWordProb)) {
					System.err.println(topics[docNum][position]);
				} else {
					ll += FastMath.log(topicWordProb);
				}
			}
		}
		
		return ll;
	}
}
