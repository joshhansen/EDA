package jhn.eda.topicdistance;

public class ConstTopicDistanceCalculator implements TopicDistanceCalculator {
	private final double distance;
	
	public ConstTopicDistanceCalculator(double distance) {
		this.distance = distance;
	}

	@Override
	public double topicDistance(int topic1, int topic2) {
		return distance;
	}

}
