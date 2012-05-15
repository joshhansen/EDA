package jhn.eda.topicdistance;

import org.apache.lucene.index.IndexReader;

public class LuceneTopicDistanceCalculator implements TopicDistanceCalculator {
	private IndexReader articleCategories;
	private IndexReader categoryCategories;
	
	public LuceneTopicDistanceCalculator(IndexReader articleCategories,
			IndexReader categoryCategories) {
		this.articleCategories = articleCategories;
		this.categoryCategories = categoryCategories;
	}

	@Override
	public double topicDistance(int topic1, int topic2) {
		// TODO Auto-generated method stub
		return 0;
	}

}
