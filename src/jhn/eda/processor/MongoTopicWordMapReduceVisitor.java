package jhn.eda.processor;

import java.util.Map.Entry;

import jhn.eda.MongoConf;

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

	public MongoTopicWordMapReduceVisitor(String labelAlphFilename, String alphFilename) {
		super(labelAlphFilename, alphFilename, MongoConf.server, MongoConf.port, MongoConf.dbName, "topic_word_counts_unreduced");
	}

	@Override
	protected void _afterLabel() {
		DBObject[] objs = new DBObject[currentLabelWordCounts.size()];
		int i = 0;
		for(Entry<String,Integer> entry : currentLabelWordCounts.entrySet()) {
			final String word = entry.getKey();
			final String count = entry.getValue().toString();
			
			final DBObject o = new BasicDBObject("w", word);
			o.put("c", new BasicDBObject(count, currentLabelIdx));
			
			objs[i++] = o;
		}
		c.insert(objs);
	}

}
