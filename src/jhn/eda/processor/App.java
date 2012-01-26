package jhn.eda.processor;

import jhn.eda.Util;

/*
 * http://code.google.com/p/java-matrix-benchmark/
 * 
 * Reqs: sparse, serializable
 * Desired: low-bandwidth (float rather than double, etc.), native
 * 
 * Colt? rows*cols < Integer.MAX_VALUE
 * MTJ? not Serializable
 * JBLAS? Can use native libs. No sparse matrix suport
 * EJML? Dense only
 * Jama? Dense only
 * 
 * 
 * UJML
 * UJML-J
 * OjAlgo
 * Parallel Colt
 * http://code.google.com/p/la4j/
 * 
 * 
 * Apache Commons Math? Serializable. Sparse.
 * 
 * MongoDB
 * 
 */

public class App {
//	private static final int LABEL_COUNT = 3550567;
//	private static final int WORD_TYPE_COUNT = 1978075;
	
	public static void main(String[] args) {
		final String srcDir = System.getenv("HOME") + "/Data/dbpedia.org/3.7";
		final String destDir = System.getenv("HOME") + "/Projects/eda_output";
		
		
		final String abstractsFilename = srcDir + "/long_abstracts_en.nt.bz2";
		final String wordIdxFilename = destDir + "/alphabet.ser";
		final String topicIdxFilename = destDir + "/labelAlphabet.ser";
		
//		final String matrixFilename = destDir + "/topicWordMatrix.ser";
		
		AbstractsProcessor ap = new AbstractsProcessor(abstractsFilename, Util.stopwords());
		ap.addVisitor(new PrintingVisitor());//Provide some console output
		ap.addVisitor(new MongoTopicWordMatrixVisitor(topicIdxFilename, wordIdxFilename));
//		ap.addVisitor(new ColtTopicWordMatrixVisitor(topicIdxFilename, wordIdxFilename, matrixFilename));
//		ap.addVisitor(new LabelIndexingVisitor(destDir+"/labelAlphabet.ser"));
//		ap.addVisitor(new WordIndexingVisitor(destDir+"/alphabet.ser"));
//		ap.addVisitor(new LabelCountingVisitor());
//		ap.addVisitor(new WordCountingVisitor());
		ap.process();
	}
}
