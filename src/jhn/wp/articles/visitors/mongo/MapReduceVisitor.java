package jhn.wp.articles.visitors.mongo;

import java.util.Collections;
import java.util.Map.Entry;

import jhn.wp.exceptions.ArticleTooShort;
import jhn.wp.visitors.mongo.MongoVisitor;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MapReduceVisitor extends MongoVisitor {
	
	private static final int MIN_LENGTH = 100;
	private DBCollection words;
	private DBCollection labels;
	private DBCollection wordLabelCounts;
	
	private int nextWordIdx = 0;
	private int nextLabelIdx = 0;

	public MapReduceVisitor(String server, int port, String dbName) {
		super(server, port, dbName);
	}

	@Override
	public void beforeEverything() throws Exception {
		super.beforeEverything();
		words = db.getCollection("words");
		labels = db.getCollection("labels");
		wordLabelCounts = db.getCollection("word_label_counts_raw");
	}

	@Override
	protected int labelIdx(String label) {
		DBObject q = new BasicDBObject("_id", label);
		DBObject r = labels.findOne(q);
		if(r == null) {
			q.put("idx", nextLabelIdx++);
			labels.save(q);
			r = q;
		}
		return (Integer) r.get("idx");
	}

	@Override
	protected int wordIdx(String word) {
		DBObject q = new BasicDBObject("_id", word);
		DBObject r = words.findOne(q);
		if(r == null) {
			q.put("idx", nextWordIdx++);
			words.save(q);
			r = q;
		}
		return (Integer) r.get("idx");
	}

	private void assertOK() throws ArticleTooShort {
		if(wordsInLabel < MIN_LENGTH) throw new ArticleTooShort(currentLabel, wordsInLabel);
	}
	
	@Override
	public void afterLabel() throws Exception {
		assertOK();
		DBObject[] objs = new DBObject[currentLabelWordCounts.size()];
		int i = 0;
		for(Entry<String,Integer> entry : currentLabelWordCounts.entrySet()) {
			final String word = entry.getKey();
			final String count = entry.getValue().toString();
			
			final DBObject o = new BasicDBObject("w", word);
			o.put("c", new BasicDBObject(count, Collections.nCopies(1, currentLabelIdx)));
			
			objs[i++] = o;
		}
		wordLabelCounts.insert(objs);
		
		super.afterLabel();
	}
}
