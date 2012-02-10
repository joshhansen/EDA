package jhn.wp.visitors.mongo;

import java.util.Collections;
import java.util.Map.Entry;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;


public class MapReduceVisitor extends MongoVisitor {
	
	private DBCollection words;
	private DBCollection labels;
	private DBCollection wordLabelCounts;
	
	private int nextWordIdx = 0;
	private int nextLabelIdx = 0;
	
	
	
	public MapReduceVisitor() {
		super();
	}

	public MapReduceVisitor(String server, int port, String dbName) {
		super(server, port, dbName);
	}

	@Override
	public void beforeEverything() {
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

	
	private boolean isOK() {
		if(wordsInLabel <= 3) return false;
		if(currentLabel.startsWith("List_of_")) return false;
		if(currentLabel.startsWith("Portal:")) return false;
		if(currentLabel.startsWith("Glossary_of_")) return false;
		if(currentLabel.startsWith("Index_of_")) return false;
		
		return true;
	}
	
	@Override
	public void afterLabel() {
		if(isOK()) {
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
		} else {
			System.out.println("Skipping " + currentLabel);
		}
		
		super.afterLabel();
	}
}
