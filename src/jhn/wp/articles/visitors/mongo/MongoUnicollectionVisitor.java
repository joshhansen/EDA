package jhn.wp.articles.visitors.mongo;

import jhn.eda.mongo.MongoConf;
import jhn.wp.visitors.mongo.MongoVisitor;

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
	public void beforeEverything() throws Exception {
		super.beforeEverything();
		c = db.getCollection(collectionName);
	}

	
}
