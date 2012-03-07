package jhn.eda.mongo;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.QueryBuilder;

//1978075

public class MongoTest {
	public static void main(String[] args) {
		try {
			Mongo m = new Mongo(MongoConf.server, MongoConf.port);
			DB db = m.getDB(MongoConf.dbName);
			DBCollection c = db.getCollection(MongoConf.collectionName);
//			c.ensureIndex("_word");
			
			DBObject q = QueryBuilder.start("food").is("pizza").get();
			DBObject u = QueryBuilder.start().push("toppings", "olives").get();
			
			System.out.println(q);
			System.out.println(u);
			
//			c.insert(q);
			
			c.update(q, u, true, false);
			
//			c.save(query);
			
//			c.insert(query);
			m.close();
		} catch(UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
