package jhn.eda.processor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jhn.eda.Util;
import cc.mallet.types.Alphabet;
import cc.mallet.types.LabelAlphabet;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

public class MongoTopicWordMatrixVisitor extends Visitor {
	private final Alphabet alphabet;
	private final LabelAlphabet labelAlphabet;
	
	private Mongo m;
	private DBCollection c;
	private String label;
	private int currentLabelIdx;
	private Map<Integer,Integer> labelWordCounts;

	public MongoTopicWordMatrixVisitor(final String labelAlphFilename, final String alphFilename) {
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
			m = new Mongo( "localhost" , 27017 );
			DB db = m.getDB("dbpedia37");
			c = db.getCollection("topicwords");
		} catch(UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void visitLabel(String label) {
		this.label = label;
		currentLabelIdx = labelAlphabet.lookupIndex(label);
		labelWordCounts = new HashMap<Integer,Integer>();
	}

	@Override
	public void visitWord(String word) {
		final Integer wordIdx = alphabet.lookupIndex(word);
		Integer count = labelWordCounts.get(wordIdx);
		count = count==null? 1 : count+1;
		labelWordCounts.put(wordIdx, count);
	}

	@Override
	public void afterLabel() {
		BasicDBObject doc = new BasicDBObject();
		doc.put("label", label);
		doc.put("labelidx", Integer.valueOf(currentLabelIdx));
		
		List<Integer> topicWordCounts = new ArrayList<Integer>(2*labelWordCounts.size());
		for(Entry<Integer,Integer> entry : labelWordCounts.entrySet()) {
			topicWordCounts.add(entry.getKey());
			topicWordCounts.add(entry.getValue());
		}
		
		doc.put("wordcounts", groupedCounts());
		
		c.insert(doc);
	}
	
	private Map<String,List<Integer>> groupedCounts() {
		Map<String,List<Integer>> groupedCounts = new HashMap<String,List<Integer>>();
		
		for(Entry<Integer,Integer> entry : labelWordCounts.entrySet()) {
			final Integer wordIdx = entry.getKey();
			final String count = entry.getValue().toString();
			List<Integer> group = groupedCounts.get(count);
			if(group == null){
				group = new ArrayList<Integer>();
				groupedCounts.put(count, group);
			}
			group.add(wordIdx);
		}
		
		return groupedCounts;
	}

	@Override
	public void afterEverything() {
		m.close();
	}
}
