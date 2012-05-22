package jhn.eda.topiccounts;

public interface TopicCounter {
	void setTotalCount(int topic, int count);
	void close();
}