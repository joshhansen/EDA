package jhn.eda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;


public class TopNTracker {
	private final Queue<Value> topNItems = new PriorityQueue<Value>();
	private final int n;
	public TopNTracker(final int n) {
		this.n = n;
	}
	

	
	public void add(double value, int position) {
		topNItems.add(new Value(value, position));
		if(topNItems.size() > n) topNItems.remove();
	}
	
	public List<Value> topN() {
		List<Value> topN = new ArrayList<Value>(n);
		while(!topNItems.isEmpty()) {
			topN.add(topNItems.remove());
		}
		Collections.reverse(topN);
		return topN;
	}
}
