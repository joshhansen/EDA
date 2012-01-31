package jhn.eda.processor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jhn.eda.Util;
import cc.mallet.types.Alphabet;
import cc.mallet.types.LabelAlphabet;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;



/*
 * Target structure:
 * {type1_idx: {topic1_idx:count, topic9_idx:count...}
 *  type2_idx: {topic44_idx:count, ...}
 *  ...
 *  }
 */
public class MongoTopicWordMatrixVisitor2 extends Visitor {
	public static final String server = "localhost";
	public static final int port = 27017;
	public static final String dbName = "dbpedia37";
	public static final String collectionName = "type_topic_counts";
	
	private final Alphabet alphabet;
	private final LabelAlphabet labelAlphabet;
	
	private Mongo m;
	private DBCollection c;
//	private String label;
	private int currentLabelIdx;
	private Map<String,Integer> labelWordCounts;

	public MongoTopicWordMatrixVisitor2(final String labelAlphFilename, final String alphFilename) {
		LabelAlphabet la = null;
		Alphabet a = null;
		try {
			System.out.print("Loading label index...");
			la = (LabelAlphabet) Util.deserialize(labelAlphFilename);
			System.out.println("done.");
			System.out.print("Loading word index...");
			a = (Alphabet) Util.deserialize(alphFilename);
			System.out.println("done.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		this.labelAlphabet = la;
		this.alphabet = a;
	}
	
	@Override
	public void beforeEverything() {
		try {
			m = new Mongo(server, port);
			DB db = m.getDB(dbName);
			c = db.getCollection(collectionName);
		} catch(UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void visitLabel(String label) {
//		this.label = label;
		currentLabelIdx = labelAlphabet.lookupIndex(label);
		labelWordCounts = new HashMap<String,Integer>();
	}

	@Override
	public void visitWord(String word) {
//		final String wordIdx = String.valueOf(alphabet.lookupIndex(word));
//		Integer count = labelWordCounts.get(wordIdx);
//		count = count==null? 1 : count+1;
//		labelWordCounts.put(wordIdx, count);
		
		Integer count = labelWordCounts.get(word);
		count = count==null? 1 : count+1;
		labelWordCounts.put(word, count);
	}

	@Override
	public void afterLabel() {
		for(Entry<String,Integer> entry : labelWordCounts.entrySet()) {
			final String word = entry.getKey();
			final Integer wordIdx = alphabet.lookupIndex(word);
			DBObject query = new BasicDBObject("_id", wordIdx);
			DBObject result = c.findOne(query);
			if(result == null) {
				query.put("_word", word);
				c.insert(query);
				result = query;
			}
			
			final String labelIdx = String.valueOf(currentLabelIdx);
			if(result.containsField(labelIdx)) throw new IllegalArgumentException();
			
			result.put(labelIdx, entry.getValue());
			c.save(result);
		}
		
//		BasicDBObject doc = new BasicDBObject();
//		doc.put("label", label);
//		doc.put("labelidx", );
//		
//		doc.put("wordcounts", labelWordCounts);
//		
//		c.insert(doc);
	}

	@Override
	public void afterEverything() {
		m.close();
	}
}
