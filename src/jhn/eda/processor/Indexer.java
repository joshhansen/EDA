package jhn.eda.processor;

import java.util.HashSet;
import java.util.Set;

import jhn.eda.Util;



public class Indexer {
	private static final int LABEL_COUNT = 3550567;
	private static final int WORD_TYPE_COUNT = 1978075;
	
	public static void main(String[] args) {
		final String outputDir = "/home/jjfresh/Projects/eda_output";
		final Set<String> stopwords = new HashSet<String>();
		for(String stopword : Util.stopwords) stopwords.add(stopword);
		
		
		final String abstractsFilename = "/home/jjfresh/Data/dbpedia.org/3.7/short_abstracts_en.nt.bz2";
		AbstractsProcessor ap = new AbstractsProcessor(abstractsFilename, stopwords);
		ap.visitors.add(new LabelIndexingVisitor(outputDir+"/labelAlphabet.ser"));
		ap.visitors.add(new WordIndexingVisitor(outputDir+"/alphabet.ser"));
//		ap.visitors.add(new LabelCountingVisitor());
//		ap.visitors.add(new WordCountingVisitor());
		ap.process();
	}
}
