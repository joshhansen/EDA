package jhn.eda;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.Randoms;

public class LuceneEDA extends EDA {
	private static final long serialVersionUID = 1L;
	
	public LuceneEDA(Alphabet targetAlphabet, LabelAlphabet topicAlphabet,
			double alphaSum, double beta, Randoms random) {
		super(targetAlphabet, topicAlphabet, alphaSum, beta, random);
	}

	@Override
	protected Map<String, List<Integer>> typeTopicCounts(String originalType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Map<String, List<Integer>> typeTopicCounts(int typeIdx) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public static void main (String[] args) throws IOException {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
//		final String alphabetFilename = outputDir + "/state_of_the_union-alphabet.ser";
		final String targetLabelAlphabetFilename = outputDir + "/dbpedia37_longabstracts_label_alphabet.ser";
		final String targetAlphabetFilename = outputDir + "/dbpedia37_longabstracts_alphabet.ser";
		
//		final String datasetFilename = System.getenv("HOME") + "/Projects/topicalguide/datasets/state_of_the_union/imported_data.mallet";
//		final String datasetFilename = System.getenv("HOME") + "/Projects/eda_java/toy_dataset2.mallet";
		final String datasetFilename = System.getenv("HOME") + "/Projects/eda_java/sotu_obama4.mallet";
		
		try {
			Alphabet targetAlphabet = (Alphabet) Util.deserialize(targetAlphabetFilename);
			System.out.print("Loading label alphabet...");
			LabelAlphabet targetLabelAlphabet = (LabelAlphabet) Util.deserialize(targetLabelAlphabetFilename);
			System.out.println("done.");
			
//			Mongo m = new Mongo(MongoConf.server, MongoConf.port);
//			DB db = m.getDB(MongoConf.dbName);
//			DBCollection c = db.getCollection("topic_word_counts_redux");
			
			InstanceList training = InstanceList.load(new File(datasetFilename));
			
			EDA eda = new LuceneEDA (targetAlphabet, targetLabelAlphabet, 50.0, 0.01, new Randoms());
			eda.addInstances(training);
			eda.sample(1000);
			
//			m.close();
		} catch(UnknownHostException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
}
