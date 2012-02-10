package jhn.wp;

import jhn.wp.visitors.PrintingVisitor;
import jhn.wp.visitors.mongo.OldMapReduceVisitor;


public class CountArticles {
	public static void main(String[] args) {
		final String srcDir = System.getenv("HOME") + "/Data/dbpedia.org/3.7";
		final String destDir = System.getenv("HOME") + "/Projects/eda_output";
		
		final String abstractsFilename = srcDir + "/long_abstracts_en.nt.bz2";
		final String wordIdxFilename = destDir + "/dbpedia37_longabstracts_alphabet.ser";
		final String topicIdxFilename = destDir + "/dbpedia37_longabstracts_label_alphabet.ser";
		
		AbstractsCounter ac = new AbstractsCounter(abstractsFilename);
		ac.addVisitor(new PrintingVisitor());
		ac.addVisitor(new OldMapReduceVisitor(topicIdxFilename, wordIdxFilename));
		ac.count();
	}
}
