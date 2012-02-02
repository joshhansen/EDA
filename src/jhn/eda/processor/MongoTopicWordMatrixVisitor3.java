package jhn.eda.processor;

import java.util.Map.Entry;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;



/*
 * Target structure:
 * {type1_idx: {count1:[topic1,topic2,topic33,...], count4:[topic9,topic4,topic848...], ...}
 *  type2_idx: {count2:[topic3], ...}
 *  ...
 *  }
 */
public class MongoTopicWordMatrixVisitor3 extends AbstractMongoTopicWordVisitor {

	public MongoTopicWordMatrixVisitor3(String labelAlphFilename, String alphFilename) {
		super(labelAlphFilename, alphFilename);
	}

	@Override
	public void beforeEverything() {
		super.beforeEverything();
		c.ensureIndex("_word");
	}

	public void _afterLabel() {
		for(Entry<String,Integer> entry : currentLabelWordCounts.entrySet()) {
			final String word = entry.getKey();
			final String count = entry.getValue().toString();
			
			DBObject q = QueryBuilder.start("_word").is(word).get();
			DBObject u = QueryBuilder.start().push(count, currentLabelIdx).get();
			c.update(q, u, true, false);
		}
	}
	

}
