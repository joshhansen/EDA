package jhn.eda.topicdistance;

import java.io.Serializable;

public class StandardMaxTopicDistanceCalculator implements MaxTopicDistanceCalculator, Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public double maxTopicDistance(int currentIteration, int maxIteration) {
		if(currentIteration < 5) {
			return Integer.MAX_VALUE;
		}
		
		double normalizedIt = (double) (currentIteration-5) / (double) maxIteration;
		return (int) (10.0 * normalizedIt);
	}

}
