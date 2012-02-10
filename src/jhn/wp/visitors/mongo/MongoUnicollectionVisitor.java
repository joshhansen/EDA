package jhn.wp.visitors.mongo;

import jhn.eda.MongoConf;

import com.mongodb.DBCollection;

public abstract class MongoUnicollectionVisitor extends MongoVisitor {
	protected String collectionName;
	protected DBCollection c;
	
	public MongoUnicollectionVisitor() {
		this(MongoConf.server, MongoConf.port, MongoConf.dbName, MongoConf.collectionName);
	}
	
	public MongoUnicollectionVisitor(String server, int port, String dbName, String collectionName) {
		super(server, port, dbName);
		this.collectionName = collectionName;
	}

	@Override
	public void beforeEverything() {
		super.beforeEverything();
		c = db.getCollection(collectionName);
	}

	
}
