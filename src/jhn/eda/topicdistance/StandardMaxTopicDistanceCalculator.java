package jhn.eda.topicdistance;

public class StandardMaxTopicDistanceCalculator implements MaxTopicDistanceCalculator {

	@Override
	public double maxTopicDistance(int currentIteration, int maxIteration) {
		if(currentIteration < 5) {
			return Integer.MAX_VALUE;
		}
		
		double normalizedIt = (double) (currentIteration-5) / (double) maxIteration;
		return (int) (10.0 * normalizedIt);
	}

}
