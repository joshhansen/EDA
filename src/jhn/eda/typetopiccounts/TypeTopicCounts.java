package jhn.eda.typetopiccounts;

import java.util.Iterator;

import jhn.eda.TopicCount;

public interface TypeTopicCounts {
	Iterator<TopicCount> typeTopicCounts(int typeIdx) throws TypeTopicCountsException;
}
