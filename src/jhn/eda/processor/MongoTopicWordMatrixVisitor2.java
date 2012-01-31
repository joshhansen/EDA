package jhn.eda.processor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
	public static final String collectionName = "type_topic_counts2";
	
	private final Alphabet alphabet;
	private final LabelAlphabet labelAlphabet;
	
	private Mongo m;
	private DBCollection c;
	private int currentLabelIdx;
	private Map<String,Integer> currentLabelWordCounts;
	private final Set<String> stopwords = Util.stopwords();
	private final WordCache cache = new WordCache();

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
		//This gets cleared in afterLabel()
		currentLabelWordCounts = new HashMap<String,Integer>();
	}

	@Override
	public void visitLabel(String label) {
		currentLabelIdx = labelAlphabet.lookupIndex(label);
	}

	@Override
	public void visitWord(String word) {
		if(stopwords.contains(word)) return;
		
		Integer count = currentLabelWordCounts.get(word);
		count = count==null? 1 : count+1;
		currentLabelWordCounts.put(word, count);
	}
	
	private class WordCache {
		private static final int size = 1000;
		private static final int numToRetain = 800;
		private final Map<String,DBObject> elements = new HashMap<String,DBObject>();
		
		private Comparator<DBObject> comp = new Comparator<DBObject>(){
			@Override
			public int compare(DBObject o1, DBObject o2) {
				return Integer.valueOf(o1.keySet().size()).compareTo(Integer.valueOf(o2.keySet().size()));
			}
		};

		public DBObject getWordFromDB(final String word) {
			//Check cache
			DBObject wordObj = elements.get(word);
			if(wordObj == null) {
				//Check database
				final Integer wordIdx = alphabet.lookupIndex(word);
				wordObj = c.findOne(new BasicDBObject("_id", wordIdx));
				if(wordObj == null) {
					wordObj = new BasicDBObject("_id", wordIdx);
					wordObj.put("_word", word);
					
					//Persist
					c.insert(wordObj);
					elements.put(word, wordObj);
				}
			}
			
			if(elements.size() >= size) flushCache();
			
			return wordObj;
		}
		
		private void printCache(DBObject[] sortedWords) {
			System.out.println("Head:");
			for(int i = sortedWords.length - 1; i > sortedWords.length - 11; i--) {
				DBObject wordObj = sortedWords[i];
				String word = wordObj.get("_word").toString();
				int count = wordObj.keySet().size();
				System.out.println("\t"+word + ": " + count);
			}
			
			System.out.println("Tail:");
			for(int i = 0; i < 10; i++) {
				DBObject wordObj = sortedWords[i];
				String word = wordObj.get("_word").toString();
				int count = wordObj.keySet().size();
				System.out.println("\t"+word + ": " + count);
			}
		}
		
		private void flushCache() {
			DBObject[] sortedWords = elements.values().toArray(new DBObject[0]);
			Arrays.sort(sortedWords, comp);
			
			printCache(sortedWords);
			
			Set<DBObject> wordsToRetain = new HashSet<DBObject>();
			
			for(int i = sortedWords.length - 1; i > (size-numToRetain); i--) {
				wordsToRetain.add(sortedWords[i]);
			}
			
//			int retained = 0;
//			for(DBObject wordObj : sortedWords) {
//				wordsToRetain.add(wordObj);
//				retained++;
//				if(retained >= numToRetain) break;
//			}
			
			Iterator<Entry<String,DBObject>> it = elements.entrySet().iterator();
			while(it.hasNext()) {
				Entry<String,DBObject> entry = it.next();
				DBObject wordObj = entry.getValue();
				if(!wordsToRetain.contains(wordObj)) {
					c.save(wordObj);
					it.remove();
				}
			}
		}
	}//end class WordCache
	
	
	
	
	@Override
	public void afterLabel() {
		for(Entry<String,Integer> entry : currentLabelWordCounts.entrySet()) {
			DBObject dbWord = cache.getWordFromDB(entry.getKey());
			
			final String labelIdx = String.valueOf(currentLabelIdx);
			if(dbWord.containsField(labelIdx)) throw new IllegalArgumentException();
			
			dbWord.put(labelIdx, entry.getValue());
			c.save(dbWord);
		}
		
		currentLabelWordCounts.clear();
	}

	@Override
	public void afterEverything() {
		m.close();
	}
}
