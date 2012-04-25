package jhn.eda;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class Config {
	private final Map<Options,Object> map = new HashMap<Options,Object>();
	
	public boolean containsKey(Options key) {
		return map.containsKey(key);
	}
	
	public void put(Options key, int value) {
		map.put(key, Integer.valueOf(value));
	}
	
	public void put(Options key, boolean value) {
		map.put(key, Boolean.valueOf(value));
	}
	
	public void put(Options key, String value) {
		map.put(key, value);
	}
	
	public int getInt(Options key) {
		return ((Integer)map.get(key)).intValue();
	}
	
	public boolean getBool(Options key) {
		return ((Boolean)map.get(key)).booleanValue();
	}
	
	public String getString(Options key) {
		return (String) map.get(key);
	}
	
	private static final Comparator<Entry<Options,Object>> itemCmptor = new Comparator<Entry<Options,Object>>(){
		@Override
		public int compare(Entry<Options, Object> o1, Entry<Options, Object> o2) {
			return o1.getKey().compareTo(o2.getKey());
		}
	};
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		
		@SuppressWarnings("unchecked")
		Entry<Options,Object>[] entries = map.entrySet().toArray(new Entry[0]);
		Arrays.sort(entries, itemCmptor);
		
		for(Entry<Options,Object> entry : entries) {
			s.append(entry.getKey());
			s.append(':');
			s.append(entry.getValue());
			s.append('\n');
		}
		
		return s.toString();
	}
}
