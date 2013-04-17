package jhn.eda.summarize;

import java.io.File;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import jhn.counts.i.i.i.IntIntIntCounterMap;
import jhn.counts.i.i.i.IntIntIntRAMCounterMap;
import jhn.eda.io.StateFileReader;
import jhn.eda.io.StateFiles;
import jhn.eda.tokentopics.DocTokenTopics;

public class Sum implements SampleSummarizer {

	@Override
	public IntIntIntCounterMap summarize(File[] fastStateFiles, Int2ObjectMap<String> sources) throws Exception {
		IntIntIntCounterMap aggregateDocTopicCounts = new IntIntIntRAMCounterMap();
		
		int docNum;
		
		for(File file : fastStateFiles) {
			System.out.println(file.getName());
			
			try(StateFileReader stateFile = StateFiles.read(file.getPath())) {
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

}
