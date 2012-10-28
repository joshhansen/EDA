/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
http://www.cs.umass.edu/~mccallum/mallet
This software is provided under the terms of the Common Public License,
version 1.0, as published by http://www.opensource.org.	For further
information, see the file `LICENSE' included with this distribution. */
package jhn.eda;

import java.io.FileNotFoundException;

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
public class EDA2 extends EDA {
	private static final long serialVersionUID = 1L;
	
	public EDA2(TopicCounts topicCountsFact, TypeTopicCounts typeTopicCounts,
			int numTopics, String logDir) throws FileNotFoundException {
		super(topicCountsFact, typeTopicCounts, numTopics, logDir);
	}
	
	@Override
	protected Runnable samplerInstance(int docNum) {
		return new DocumentSampler2(this, docNum);
	}

	private class DocumentSampler2 extends DocumentSampler {
		
		public DocumentSampler2(EDA eda, int docNum) {
			eda.super(docNum);
		}

		private int maybeMinusOne(int x) {
			if(x > 1) return x-1;
			return 0;
		}
		
		@Override
		protected double completeConditional(TopicCount ttc, int oldTopic, int[] docTopicCounts) {
			return ttc.count*(maybeMinusOne(docTopicCounts[ttc.topic]) + alphas[docNum]);
		}
	}//end class DocumentSampler2
}
