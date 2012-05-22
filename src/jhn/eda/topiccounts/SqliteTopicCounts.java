package jhn.eda.topiccounts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jhn.util.Factory;

public class SqliteTopicCounts implements TopicCounts, TopicCounter {
	public static Factory<TopicCounts> factory(final String dbFilename) {
		return new Factory<TopicCounts>(){
			@Override
			public TopicCounts create() {
				return new SqliteTopicCounts(dbFilename);
			}
		};
	}
	
	private Connection c;
	private PreparedStatement selectStmt;
	private PreparedStatement insertStmt;
	private int insertCount = 0;
	public SqliteTopicCounts(String destFilename) {
		 try {
			 Class.forName("org.sqlite.JDBC");
			 c = DriverManager.getConnection("jdbc:sqlite:" + destFilename);
			 Statement s = c.createStatement();
			 s.executeUpdate("CREATE TABLE IF NOT EXISTS topiccount (id INTEGER PRIMARY KEY, total_count INTEGER NOT NULL)");
			 s.close();
			 
			 c.setAutoCommit(false);
			 
			 selectStmt = c.prepareStatement("SELECT total_count FROM topiccount WHERE id=?");
			 insertStmt = c.prepareStatement("INSERT INTO topiccount VALUES(?,?)");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setTotalCount(int topicID, int count) {
		try {
			insertStmt.setInt(1, topicID);
			insertStmt.setInt(2, count);
			insertStmt.addBatch();
			
			if(insertCount > 0 && insertCount % 10000 == 0) {
				insertStmt.executeBatch();
				c.commit();
			}
			insertCount++;
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public int topicCount(int topicID) throws TopicCountsException {
		try {
			selectStmt.setInt(1, topicID);
			ResultSet rs = selectStmt.executeQuery();
			rs.next();
			int totalCount = rs.getInt("total_count");
			rs.close();
			return totalCount;
		} catch(SQLException e) {
			throw new TopicCountsException(e);
		}
	}
	
	@Override
	public void close() {
		try {
			insertStmt.executeBatch();
			c.commit();
			selectStmt.close();
			insertStmt.close();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


}