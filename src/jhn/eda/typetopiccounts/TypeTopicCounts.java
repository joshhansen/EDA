package jhn.eda.typetopiccounts;

import java.util.Iterator;


public interface TypeTopicCounts {
	Iterator<TypeTopicCount> typeTopicCounts(int typeIdx) throws TypeTopicCountsException;
}
