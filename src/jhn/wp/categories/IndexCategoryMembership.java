package jhn.wp.categories;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.QueryBuilder;

public class IndexCategoryMembership {
//	private IndexReader r;
//	private final IndexWriter w;
//	public IndexCategoryMembership(final String luceneDir) {
//		IndexReader r = null;
//		IndexWriter w = null;
//		try {
//			FSDirectory dir = FSDirectory.open(new File(luceneDir));
//			r = IndexReader.open(dir, true);
//			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);
//			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, analyzer);
//			w = new IndexWriter(dir, config);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		this.r = r;
//		this.w = w;
//	}
	

	//<http://dbpedia.org/resource/Aristotle> <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:Ancient_Greek_philosophers> .
	private static final String articleS = "<http://dbpedia\\.org/resource/([^>]+)> <http://purl\\.org/dc/terms/subject> <http://dbpedia\\.org/resource/(Category:[^>]+)> \\.";
	private static final Pattern articleRgx = Pattern.compile(articleS);
	private void indexArticleCategories(final String srcFilename, final String destMongoServer, final int destMongoPort, final String destMongoDbName) {
		try {
			final Mongo destMongo = new Mongo(destMongoServer, destMongoPort);
			final DB dest = destMongo.getDB(destMongoDbName);
			
			final DBCollection parents = dest.getCollection("parentCategories");
			final DBCollection children = dest.getCollection("categoryChildren");
		
			BufferedReader r = new BufferedReader(new FileReader(srcFilename));
			String line = null;
			while( (line=r.readLine()) != null) {
				Matcher m = articleRgx.matcher(line);
				if(m.matches()) {
					final String article = m.group(1);
					final String category = m.group(2);
					
					
//					QueryBuilder.start("_id").is(article).
//					DBObject q = new BasicDBObject("_id", article);
//					
//					
//					DBObject r = words.findOne(q);
//					if(r == null) {
//						q.put("idx", nextWordIdx++);
//						words.save(q);
//						r = q;
//					}
					
				} else {
					throw new IllegalArgumentException();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	//<http://dbpedia.org/resource/Category:World_War_II> <http://www.w3.org/2004/02/skos/core#broader> <http://dbpedia.org/resource/Category:Wars_involving_the_Soviet_Union> .	
	private static final String categoryS = "<http://dbpedia\\.org/resource/(Category:[^>]+)> <http://www\\.w3\\.org/2004/02/skos/core#broader> <http://dbpedia\\.org/resource/(Category:[^>]+)> \\.";
	private void indexCategoryCategories() {
		
	}
}
