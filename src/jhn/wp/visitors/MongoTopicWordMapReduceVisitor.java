package jhn.wp.visitors;

import java.util.Collections;
import java.util.Map.Entry;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


/**
 * Target Structure:
 * 
 * { word1: {count: [topic]},
 *   word2: {count2: [topic2]},
 *   ...
 * }
 *
 */
public class MongoTopicWordMapReduceVisitor extends AbstractMongoTopicWordVisitor {
	
	@Override
	public void beforeEverything() {
		super.beforeEverything();
		c.ensureIndex("db.w");
	}
	
	public MongoTopicWordMapReduceVisitor(String labelAlphFilename, String alphFilename, String server, int port,
			String dbName, String collectionName) {
		super(labelAlphFilename, alphFilename, server, port, dbName, collectionName);
	}

	public MongoTopicWordMapReduceVisitor(String labelAlphFilename, String alphFilename) {
		super(labelAlphFilename, alphFilename);
	}

	private boolean isOK() {
		if(wordsInLabel <= 3) return false;
		if(currentLabel.startsWith("List_of_")) return false;
		if(currentLabel.startsWith("Portal:")) return false;
		if(currentLabel.startsWith("Glossary_of_")) return false;
		
		return true;
	}
	
	@Override
	protected void _afterLabel() {
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
			c.insert(objs);
		} else {
			System.out.println("Skipping " + currentLabel);
		}
	}

}
