package jhn.wp;

import jhn.eda.MongoConf;
import jhn.wp.visitors.mongo.MapReduceVisitor;


public class CountArticles {
	public static void main(String[] args) {
		final String srcDir = System.getenv("HOME") + "/Data/wikipedia.org";
		final String articlesFilename = srcDir + "/enwiki-20121122-pages-articles.xml.bz2";
		
		CorpusCounter ac = new ArticlesCounter(articlesFilename);
//		ac.addVisitor(new PrintingVisitor());
		ac.addVisitor(new MapReduceVisitor(MongoConf.server, MongoConf.port, "wp"));
		ac.count();
	}
}
