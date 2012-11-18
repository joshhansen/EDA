package jhn.eda.summarize;

import java.io.File;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import jhn.counts.i.i.IntIntCounter;
import jhn.counts.i.i.IntIntRAMCounter;
import jhn.counts.i.i.i.IntIntIntCounterMap;
import jhn.counts.i.i.i.IntIntIntRAMCounterMap;
import jhn.eda.tokentopics.DocTokenTopics;
import jhn.eda.tokentopics.FastStateFileReader;

public class MajoritySampleSummarizer implements SampleSummarizer {

	@Override
	public IntIntIntCounterMap summarize(File[] fastStateFiles, Int2ObjectMap<String> sources) throws Exception {
		IntIntIntCounterMap summary = new IntIntIntRAMCounterMap();
		
		FastStateFileReader[] readers = new FastStateFileReader[fastStateFiles.length];
		for(int i = 0; i < fastStateFiles.length; i++) {
			readers[i] = new FastStateFileReader(fastStateFiles[i].getPath());
		}
		
		int docNum;
		DocTokenTopics[] docSamples = new DocTokenTopics[readers.length];
		while(readers[0].hasNext()) {
			for(int i = 0; i < readers.length; i++) {
				if(i > 0) {
					if(!readers[i].hasNext()) {
						throw new IllegalStateException();
					}
				}
				docSamples[i] = readers[i].next();
			}
			docNum = docSamples[0].docNum();
			sources.put(docNum, docSamples[0].docSource());
			System.out.println(docNum);
			
			IntList majorityTopics = majorityTopics(docSamples);
			for(int topic : majorityTopics) {
				summary.inc(docNum, topic);
			}
		}
		
		return summary;
	}
	
	private static IntList majorityTopics(DocTokenTopics[] docSamples) {
		IntList majorityTopics = new IntArrayList();
		int[] topics = new int[docSamples.length];
		
		while(docSamples[0].hasNext()) {
			for(int i = 0; i < docSamples.length; i++) {
				if(i > 0) {
					if(!docSamples[i].hasNext()) {
						throw new IllegalStateException();
					}
				}
				topics[i] = docSamples[i].nextInt();
			}
			majorityTopics.add(majorityTopic(topics));
		}
		
		return majorityTopics;
	}
	
	private static int majorityTopic(int[] samples) {
		IntIntCounter counts = new IntIntRAMCounter();
		for(int topic : samples) {
			counts.inc(topic);
		}
		return counts.fastTopN(1).get(0).getIntKey();
	}

	@Override
	public String name() {
		return "majority";
	}

}
