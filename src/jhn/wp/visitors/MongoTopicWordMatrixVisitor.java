package jhn.wp.visitors;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class MongoTopicWordMatrixVisitor extends AbstractMongoTopicWordVisitor {
	public MongoTopicWordMatrixVisitor(String labelAlphFilename, String alphFilename) {
		super(labelAlphFilename, alphFilename);
	}
	
	public MongoTopicWordMatrixVisitor(String labelAlphFilename, String alphFilename, String server, int port,
			String dbName, String collectionName) {
		super(labelAlphFilename, alphFilename, server, port, dbName, collectionName);
	}

	@Override
	public void visitWord(String word) {
//		final Integer wordIdx = alphabet.lookupIndex(word);
		final String wordIdx = String.valueOf(alphabet.lookupIndex(word));
		Integer count = currentLabelWordCounts.get(wordIdx);
		if(count == null) {
			ensureIdx(wordIdx);
			count = 1;
		} else {
			count++;
		}
		currentLabelWordCounts.put(wordIdx, count);
	}

	private static final DBObject sparse = new BasicDBObject("sparse", true);
	private void ensureIdx(String wordIdx) {
		c.ensureIndex(new BasicDBObject(wordIdx, 1), sparse);
	}
	@Override
	public void _afterLabel() {
		BasicDBObject doc = new BasicDBObject();
		doc.put("label", currentLabel);
		doc.put("labelidx", Integer.valueOf(currentLabelIdx));
		
//		List<Integer> topicWordCounts = new ArrayList<Integer>(2*labelWordCounts.size());
//		for(Entry<Integer,Integer> entry : labelWordCounts.entrySet()) {
//			topicWordCounts.add(entry.getKey());
//			topicWordCounts.add(entry.getValue());
//		}
//		
//		doc.put("wordcounts", groupedCounts());
		
		doc.put("wordcounts", currentLabelWordCounts);
		
		c.insert(doc);
	}
	
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

	@Override
	public void afterEverything() {
		m.close();
	}
}
