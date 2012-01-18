package jhn.eda.processor;

import java.util.HashSet;
import java.util.Set;

import cern.colt.matrix.DoubleMatrix2D;

import jhn.eda.Util;



public class Indexer {
	private static final int LABEL_COUNT = 3550567;
	private static final int WORD_TYPE_COUNT = 1978075;
	
	public static void main(String[] args) {
		DoubleMatrix2D m;
		final String outputDir = "/home/jjfresh/Projects/eda_output";
		final Set<String> stopwords = new HashSet<String>();
		for(String stopword : Util.stopwords) stopwords.add(stopword);
		
		
		final String abstractsFilename = "/home/jjfresh/Data/dbpedia.org/3.7/short_abstracts_en.nt.bz2";
		AbstractsProcessor ap = new AbstractsProcessor(abstractsFilename, stopwords);
		ap.addVisitor(new LabelIndexingVisitor(outputDir+"/labelAlphabet.ser"));
		ap.addVisitor(new WordIndexingVisitor(outputDir+"/alphabet.ser"));
//		ap.addVisitor(new LabelCountingVisitor());
//		ap.addVisitor(new WordCountingVisitor());
		ap.process();
	}
}
