package jhn.eda.summarize;

import java.io.File;
import java.io.IOException;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import jhn.counts.i.i.i.IntIntIntCounterMap;
import jhn.counts.i.i.i.IntIntIntRAMCounterMap;
import jhn.eda.io.FastStateFileReader;
import jhn.eda.tokentopics.DocTokenTopics;

public class SumSampleSummarizer implements SampleSummarizer {

	@Override
	public IntIntIntCounterMap summarize(File[] fastStateFiles, Int2ObjectMap<String> sources) throws IOException {
		IntIntIntCounterMap aggregateDocTopicCounts = new IntIntIntRAMCounterMap();
		
		int docNum;
		
		for(File file : fastStateFiles) {
			System.out.println(file.getName());
			
			try(FastStateFileReader stateFile = new FastStateFileReader(file.getPath())) {
				for(DocTokenTopics topics : stateFile) {
					docNum = topics.docNum();
					sources.put(docNum, topics.docSource());
					while(topics.hasNext()) {
						aggregateDocTopicCounts.inc(docNum, topics.nextInt());
					}
				}
			}
		}
		
		return aggregateDocTopicCounts;
	}

	@Override
	public String name() {
		return "sum";
	}

}
