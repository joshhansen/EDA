package jhn.wp.visitors.mongo;

import jhn.eda.mongo.MongoConf;
import jhn.wp.exceptions.SkipException;
import jhn.wp.visitors.LabelAggregatingVisitor;

import com.mongodb.DB;
import com.mongodb.Mongo;

public abstract class MongoVisitor extends LabelAggregatingVisitor {

	protected final String server;
	protected final int port;
	protected final String dbName;
	protected Mongo m;
	protected DB db;
	
	protected String currentLabel;
	protected int currentLabelIdx;

	public MongoVisitor() {
		this(MongoConf.server, MongoConf.port, MongoConf.dbName);
	}
	
	public MongoVisitor(String server, int port, String dbName) {
		this.server = server;
		this.port = port;
		this.dbName = dbName;
	}

	@Override
	public void beforeEverything() throws Exception {
		super.beforeEverything();
		m = new Mongo(server, port);
		db = m.getDB(dbName);
	}

	@Override
	public void visitLabel(String label) throws SkipException {
		super.visitLabel(label);
		currentLabel = label;
		currentLabelIdx = labelIdx(label);
	}
	
	protected abstract int labelIdx(String label);
	
	protected abstract int wordIdx(String word);

	@Override
	public void afterEverything() {
		m.close();
	}

}