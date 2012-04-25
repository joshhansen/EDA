package jhn.eda;

public class Value implements Comparable<Value> {
	double value;
	int position;
	
	public Value(double value, int position) {
		this.value = value;
		this.position = position;
	}

	@Override
	public int compareTo(Value o) {
		return Double.compare(value, o.value);
	}
}
