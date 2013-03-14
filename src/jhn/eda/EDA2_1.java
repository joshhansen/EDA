/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.	For further
information, see the file `LICENSE' included with this distribution. */
package jhn.eda;

import java.io.FileNotFoundException;

import jhn.counts.i.i.IntIntCounter;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.typetopiccounts.TopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;

/**
* An implementation of Explicit Dirichlet Allocation using Gibbs sampling. Based on SimpleLDA by David Mimno and Andrew
* McCallum.
* 
* @author Josh Hansen
* @author David Mimno
* @author Andrew McCallum
*/
public class EDA2_1 extends EDA {
	private static final long serialVersionUID = 1L;
	
	public EDA2_1(TopicCounts topicCountsFact, TypeTopicCounts typeTopicCounts,
			int numTopics, String logDir) throws FileNotFoundException {
		super(topicCountsFact, typeTopicCounts, numTopics, logDir);
	}
	
	@Override
	protected DocumentSampler samplerInstance(int docNum) {
		return new DocumentSampler2(this, docNum);
	}

	private class DocumentSampler2 extends DocumentSampler {
		
		public DocumentSampler2(EDA eda, int docNum) {
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
			topicCount = topicCounts.topicCount(ttc.topic);
			topicWordProb = topicCount != 0 ? typeTopicCount / topicCount : 0.0;
			docTopicCount = docTopicCounts.getCount(ttc.topic) - (oldTopic==ttc.topic ? 1.0 : 0.0);
			score = topicWordProb*(docTopicCount + alphas[ttc.topic]);
			if(Double.isInfinite(score)) {
				System.err.println("Problem");
			}
			return score;
		}
	}//end class DocumentSampler2
}
