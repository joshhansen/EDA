package jhn.wp;

import jhn.wp.visitors.lucene.LuceneVisitor;


public class CountArticles {
	public static void main(String[] args) {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
		final String luceneDir = outputDir + "/wp_lucene3";
		final String errorLog = outputDir + "/wp_lucene3.log";
		
		final String srcDir = System.getenv("HOME") + "/Data/wikipedia.org";
		final String articlesFilename = srcDir + "/enwiki-20121122-pages-articles.xml.bz2";
		
		
		CorpusCounter ac = new ArticlesCounter(articlesFilename, errorLog);
//		ac.addVisitor(new PrintingVisitor());
//		ac.addVisitor(new MapReduceVisitor(MongoConf.server, MongoConf.port, "wp"));
		ac.addVisitor(new LuceneVisitor(luceneDir));
		ac.count();
	}
}
