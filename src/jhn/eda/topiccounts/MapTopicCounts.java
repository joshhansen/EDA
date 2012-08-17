package jhn.eda.topiccounts;

import java.io.File;

import jhn.counts.i.i.IntIntCounter;
import jhn.counts.i.i.IntIntRAMCounter;
import jhn.util.Factory;
import jhn.util.Util;

public class MapTopicCounts implements TopicCounter,TopicCounts,AutoCloseable {
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
	
	private IntIntCounter counts;
	private boolean creating;

	public MapTopicCounts(String outputFilename) {
		this.outputFilename = outputFilename;
		creating = ! new File(outputFilename).exists();
		if(creating) {
			counts = new IntIntRAMCounter();
		} else {
			try {
				counts = (IntIntCounter) Util.deserialize(outputFilename);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public void setTotalCount(int topic, int count) {
		counts.set(topic, count);
	}

	@Override
	public void close() {
		if(creating) {
			Util.serialize(counts, outputFilename);
		}
	}

	@Override
	public int topicCount(int topicID) {
		return counts.getCount(topicID);
	}

}
