package jhn.eda.processor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jhn.eda.MongoConf;
import jhn.eda.Util;
import cc.mallet.types.Alphabet;
import cc.mallet.types.LabelAlphabet;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.QueryBuilder;



/*
 * Target structure:
 * {type1_idx: {count1:[topic1,topic2,topic33,...], count4:[topic9,topic4,topic848...], ...}
 *  type2_idx: {count2:[topic3], ...}
 *  ...
 *  }
 */
public class MongoTopicWordMatrixVisitor3 extends Visitor {
	private final Alphabet alphabet;
	private final LabelAlphabet labelAlphabet;
	
	private Mongo m;
	private DBCollection c;
	private DB db;
	private int currentLabelIdx;
	private Map<String,Integer> currentLabelWordCounts;
	private final Set<String> stopwords = Util.stopwords();

	public MongoTopicWordMatrixVisitor3(final String labelAlphFilename, final String alphFilename) {
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
			m = new Mongo(MongoConf.server, MongoConf.port);
			db = m.getDB(MongoConf.dbName);
			c = db.getCollection(MongoConf.collectionName);
			c.ensureIndex("_word");
		} catch(UnknownHostException e) {
			e.printStackTrace();
		}
		//This gets cleared in afterLabel()
		currentLabelWordCounts = new HashMap<String,Integer>();
		
		
	}

	@Override
	public void visitLabel(String label) {
		currentLabelIdx = labelAlphabet.lookupIndex(label);
	}

	@Override
	public void visitWord(String word) {
		if(word.isEmpty() || stopwords.contains(word)) return;
		
		Integer count = currentLabelWordCounts.get(word);
		count = count==null? 1 : count+1;
		currentLabelWordCounts.put(word, count);
	}
	
	@Override
	public void afterLabel() {
		for(Entry<String,Integer> entry : currentLabelWordCounts.entrySet()) {
			final String word = entry.getKey();
			final String count = entry.getValue().toString();
			
			DBObject q = QueryBuilder.start("_word").is(word).get();
			DBObject u = QueryBuilder.start().push(count, currentLabelIdx).get();
			c.update(q, u, true, false);
		}
		
		currentLabelWordCounts.clear();
	}
	
	private void assignWordIndexes() {
		System.out.print("Assigning word indexes...");
		for(DBObject wordObj : c.find()) {
			final String word = wordObj.get("_word").toString();
			final Integer wordIdx = alphabet.lookupIndex(word);
			wordObj.put("_wordidx", wordIdx);
			c.save(wordObj);
		}
		System.out.println("done.");
	}

	@Override
	public void afterEverything() {
		assignWordIndexes();
		m.close();
	}
}
