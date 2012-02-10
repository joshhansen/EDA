package jhn.wp.visitors.mongo;

import java.io.FileNotFoundException;
import java.io.IOException;

import jhn.eda.MongoConf;
import jhn.eda.Util;
import cc.mallet.types.Alphabet;
import cc.mallet.types.LabelAlphabet;


public abstract class MongoAlphabeticalVisitor extends MongoUnicollectionVisitor{
	protected final Alphabet alphabet;
	protected final LabelAlphabet labelAlphabet;
	
	public MongoAlphabeticalVisitor(String labelAlphFilename, String alphFilename) {
		this(labelAlphFilename, alphFilename, MongoConf.server, MongoConf.port, MongoConf.dbName, MongoConf.collectionName);
	}

	public MongoAlphabeticalVisitor(String labelAlphFilename, String alphFilename,
			String server, int port, String dbName, String collectionName) {
		super(server, port, dbName, collectionName);
		
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
	protected int labelIdx(String label) {
		return labelAlphabet.lookupIndex(label);
	}

	@Override
	protected int wordIdx(String word) {
		return alphabet.lookupIndex(word);
	}
}
