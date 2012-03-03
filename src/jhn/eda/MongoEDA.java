package jhn.eda;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.Randoms;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class MongoEDA extends EDA {
	private static final long serialVersionUID = 1L;
	
	private DBCollection c;
	private DBObject result;
	private final DBObject query = new BasicDBObject();
	
	public MongoEDA(DBCollection c, LabelAlphabet topicAlphabet,
			double alphaSum, double beta, Randoms random) {
		super(topicAlphabet, alphaSum, beta, random);
		this.c = c;
	}

	@Override
	protected Map<String,List<Integer>> typeTopicCounts(String originalType) {
		query.put("_id", originalType);
		result = c.findOne(query);
		
		if(result == null) throw new IllegalArgumentException("\nFound no counts for type '" + originalType + "'");
		
		return (Map<String, List<Integer>>) result.get("value");
	}

	@Deprecated
	@Override
	protected Map<String,List<Integer>> typeTopicCounts(int typeIdx) {
		return typeTopicCounts(alphabet.lookupObject(typeIdx).toString());
	}
	
	public static void main (String[] args) throws IOException {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
//		final String alphabetFilename = outputDir + "/state_of_the_union-alphabet.ser";
		final String targetLabelAlphabetFilename = outputDir + "/dbpedia37_longabstracts_label_alphabet.ser";
//		final String targetAlphabetFilename = outputDir + "/dbpedia37_longabstracts_alphabet.ser";
		
//		final String datasetFilename = System.getenv("HOME") + "/Projects/topicalguide/datasets/state_of_the_union/imported_data.mallet";
//		final String datasetFilename = System.getenv("HOME") + "/Projects/eda_java/toy_dataset2.mallet";
		final String datasetFilename = System.getenv("HOME") + "/Projects/eda_java/sotu_obama4.mallet";
		
		try {
//			Alphabet targetAlphabet = (Alphabet) Util.deserialize(targetAlphabetFilename);
			System.out.print("Loading label alphabet...");
			LabelAlphabet targetLabelAlphabet = (LabelAlphabet) Util.deserialize(targetLabelAlphabetFilename);
			System.out.println("done.");
			
			Mongo m = new Mongo(MongoConf.server, MongoConf.port);
			DB db = m.getDB(MongoConf.dbName);
			DBCollection c = db.getCollection("topic_word_counts_redux");
			
			InstanceList training = InstanceList.load(new File(datasetFilename));
			
			MongoEDA eda = new MongoEDA (c, targetLabelAlphabet, 50.0, 0.01, new Randoms());
			eda.addInstances(training);
			eda.sample(1000);
			
			m.close();
		} catch(UnknownHostException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
}
