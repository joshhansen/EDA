package jhn.wp.articles;

import jhn.wp.AbstractsProcessor;
//import jhn.wp.articles.visitors.mongo.OldMapReduceVisitor;
import jhn.wp.visitors.PrintingVisitor;


/**
 * Index DBPedia article abstracts
 */
public class IndexAbstracts {
//	private static final int LABEL_COUNT = 3550567;
//	private static final int WORD_TYPE_COUNT = 1978075;
	
	public static void main(String[] args) {
		final String srcDir = System.getenv("HOME") + "/Data/dbpedia.org/3.7";
		final String name = "eda_output";
		final String destDir = System.getenv("HOME") + "/Projects/" + name;
		final String logFilename = destDir + "/" + name + ".log";
		final String errLogFilename = destDir + "/" + name + ".error.log";
		
		final String abstractsFilename = srcDir + "/long_abstracts_en.nt.bz2";
		final String wordIdxFilename = destDir + "/dbpedia37_longabstracts_alphabet.ser";
		final String topicIdxFilename = destDir + "/dbpedia37_longabstracts_label_alphabet.ser";
		
		AbstractsProcessor ac = new AbstractsProcessor(abstractsFilename, logFilename, errLogFilename);
		ac.addVisitor(new PrintingVisitor());//Provide some console output
//		ac.addVisitor(new OldMapReduceVisitor(topicIdxFilename, wordIdxFilename));
//		ap.addVisitor(new LabelIndexingVisitor(destDir+"/labelAlphabet.ser"));
//		ap.addVisitor(new WordIndexingVisitor(destDir+"/alphabet.ser"));
//		ap.addVisitor(new LabelCountingVisitor());
//		ap.addVisitor(new WordCountingVisitor());
		ac.count();
	}
}