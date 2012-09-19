package jhn.eda.typetopiccounts;

import java.util.Iterator;


public interface TypeTopicCounts {
	Iterator<TopicCount> typeTopicCounts(int typeIdx) throws TypeTopicCountsException;
}
