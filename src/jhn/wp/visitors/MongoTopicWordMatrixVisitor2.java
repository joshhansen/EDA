package jhn.wp.visitors;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;



/*
 * Target structure:
 * {type1_idx: {topic1_idx:count, topic9_idx:count...}
 *  type2_idx: {topic44_idx:count, ...}
 *  ...
 *  }
 */
public class MongoTopicWordMatrixVisitor2 extends AbstractMongoTopicWordVisitor {
	private final WordCache cache = new WordCache();



	public MongoTopicWordMatrixVisitor2(String labelAlphFilename,
			String alphFilename, String server, int port, String dbName,
			String collectionName) {
		super(labelAlphFilename, alphFilename, server, port, dbName, collectionName);
		// TODO Auto-generated constructor stub
	}

	public MongoTopicWordMatrixVisitor2(String labelAlphFilename,
			String alphFilename) {
		super(labelAlphFilename, alphFilename);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void _afterLabel() {
		for(Entry<String,Integer> entry : currentLabelWordCounts.entrySet()) {
			DBObject dbWord = cache.getWordFromDB(entry.getKey());
			
			final String labelIdx = String.valueOf(currentLabelIdx);
			if(dbWord.containsField(labelIdx)) throw new IllegalArgumentException();
			
			dbWord.put(labelIdx, entry.getValue());
			c.save(dbWord);
		}
	}

	@Override
	public void afterEverything() {
		cache.close();
		super.afterEverything();
	}
	
	private class WordCache {
		private static final int size = 5000;
		private static final int numToRetain = 1000;
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
		
//		private void printCache(DBObject[] sortedWords) {
//			System.out.println("Head:");
//			for(int i = sortedWords.length - 1; i > sortedWords.length - 11; i--) {
//				DBObject wordObj = sortedWords[i];
//				String word = wordObj.get("_word").toString();
//				int count = wordObj.keySet().size();
//				System.out.println("\t"+word + ": " + count);
//			}
//			
//			System.out.println("Tail:");
//			for(int i = 0; i < 10; i++) {
//				DBObject wordObj = sortedWords[i];
//				String word = wordObj.get("_word").toString();
//				int count = wordObj.keySet().size();
//				System.out.println("\t"+word + ": " + count);
//			}
//		}
		
		private void flushCache() {
			DBObject[] sortedWords = elements.values().toArray(new DBObject[0]);
			Arrays.sort(sortedWords, comp);
			
//			printCache(sortedWords);
			
			Set<DBObject> wordsToRetain = new HashSet<DBObject>();
			
			for(int i = sortedWords.length - 1; i > (size-numToRetain); i--) {
				wordsToRetain.add(sortedWords[i]);
			}
			
			
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
		
		public void close() {
			Iterator<Entry<String,DBObject>> it = elements.entrySet().iterator();
			while(it.hasNext()) {
				Entry<String,DBObject> entry = it.next();
				DBObject wordObj = entry.getValue();
				c.save(wordObj);
				it.remove();
			}
		}
	}//end class WordCache
	
//	private Map<String,List<Integer>> groupedCounts() {
//		Map<String,List<Integer>> groupedCounts = new HashMap<String,List<Integer>>();
//		
//		for(Entry<Integer,Integer> entry : labelWordCounts.entrySet()) {
//			final Integer wordIdx = entry.getKey();
//			final String count = entry.getValue().toString();
//			List<Integer> group = groupedCounts.get(count);
//			if(group == null){
//				group = new ArrayList<Integer>();
//				groupedCounts.put(count, group);
//			}
//			group.add(wordIdx);
//		}
//		
//		return groupedCounts;
//	}
}
