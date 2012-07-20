package jhn.eda.topiccounts;

import java.io.Closeable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jhn.util.Factory;
import jhn.util.Util;

public class MapTopicCounts implements TopicCounter,TopicCounts,Closeable {
	public static Factory<TopicCounts> factory(final String filename) {
		final TopicCounts tcs = new MapTopicCounts(filename);
		return new Factory<TopicCounts>(){
			@Override
			public TopicCounts create() {
				return tcs;
			}
		};
	}
	
	
	private final String outputFilename;
	private Map<Integer,Integer> counts;
	private boolean creating;
	public MapTopicCounts(String outputFilename) {
		this.outputFilename = outputFilename;
		creating = ! new File(outputFilename).exists();
		if(creating) {
			counts = new HashMap<>();
		} else {
			try {
				counts = (Map<Integer, Integer>) Util.deserialize(outputFilename);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public void setTotalCount(int topic, int count) {
		counts.put(topic, count);
	}

	@Override
	public void close() {
		if(creating) {
			Util.serialize(counts, outputFilename);
		}
	}

	@Override
	public int topicCount(int topicID) throws TopicCountsException {
		Integer count = counts.get(topicID);
		if(count == null) {
			throw new TopicCountsException();
		}
		return count.intValue();
	}

}
