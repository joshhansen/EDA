package jhn.wp.articles;

import jhn.wp.ArticlesCounter;
import jhn.wp.CorpusCounter;
import jhn.wp.visitors.LuceneVisitor;


public class CountArticles {
	public static void main(String[] args) {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
		final String name = "wp_lucene4";
		final String luceneDir = outputDir + "/" + name;
		final String logFilename = outputDir + "/" + name + ".log";
		final String errLogFilename = outputDir + "/" + name + ".error.log";
		
		final String srcDir = System.getenv("HOME") + "/Data/wikipedia.org";
		final String articlesFilename = srcDir + "/enwiki-20121122-pages-articles.xml.bz2";
		
		
		CorpusCounter ac = new ArticlesCounter(articlesFilename, logFilename, errLogFilename);
//		ac.addVisitor(new PrintingVisitor());
//		ac.addVisitor(new MapReduceVisitor(MongoConf.server, MongoConf.port, "wp"));
		ac.addVisitor(new LuceneVisitor(luceneDir));
		ac.count();
	}
}
