package jhn.wp.visitors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jhn.eda.MongoConf;
import jhn.eda.Util;
import cc.mallet.types.Alphabet;
import cc.mallet.types.LabelAlphabet;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public abstract class AbstractMongoTopicWordVisitor extends Visitor{
	private final String server;
	private final int port;
	private final String dbName;
	private final String collectionName;
	
	protected final Alphabet alphabet;
	protected final LabelAlphabet labelAlphabet;
	
	protected Mongo m;
	protected DB db;
	protected DBCollection c;
	
	protected String currentLabel;
	protected int currentLabelIdx;
	protected int wordsInLabel = 0;
	protected Map<String,Integer> currentLabelWordCounts;
	protected final Set<String> stopwords = Util.stopwords();

	public AbstractMongoTopicWordVisitor(final String labelAlphFilename, final String alphFilename) {
		this(labelAlphFilename, alphFilename, MongoConf.server, MongoConf.port, MongoConf.dbName, MongoConf.collectionName);
	}
	
	public AbstractMongoTopicWordVisitor(final String labelAlphFilename, final String alphFilename, String server, int port, String dbName, String collectionName) {
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
		
		this.server = server;
		this.port = port;
		this.dbName = dbName;
		this.collectionName = collectionName;
	}

	@Override
	public void beforeEverything() {
		try {
			m = new Mongo(server, port);
			db = m.getDB(dbName);
			c = db.getCollection(collectionName);
		} catch(UnknownHostException e) {
			e.printStackTrace();
		}
		//This gets cleared in afterLabel()
		currentLabelWordCounts = new HashMap<String,Integer>();
	}
	
	@Override
	public void visitLabel(String label) {
		currentLabel = label;
		currentLabelIdx = labelAlphabet.lookupIndex(label);
		wordsInLabel = 0;
	}
	
	@Override
	public void visitWord(String word) {
		if(word.isEmpty() || stopwords.contains(word)) return;
		
		Integer count = currentLabelWordCounts.get(word);
		count = count==null? 1 : count+1;
		currentLabelWordCounts.put(word, count);
		wordsInLabel++;
	}
	
	@Override
	public void afterLabel() {
		_afterLabel();
		currentLabelWordCounts.clear();
	}
	
	protected abstract void _afterLabel();
	
//	private void assignWordIndexes() {
//		System.out.print("Assigning word indexes...");
//		for(DBObject wordObj : c.find()) {
//			final String word = wordObj.get("w").toString();
//			final Integer wordIdx = alphabet.lookupIndex(word);
//			wordObj.put("_widx", wordIdx);
//			c.save(wordObj);
//		}
//		System.out.println("done.");
//	}

	@Override
	public void afterEverything() {
//		assignWordIndexes();
		m.close();
	}
}
