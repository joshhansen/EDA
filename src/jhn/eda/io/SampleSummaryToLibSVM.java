package jhn.eda.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jhn.eda.tokentopics.DocTopicCounts;
import jhn.eda.typetopiccounts.TopicCount;
import jhn.io.LibSVMFileWriter;

public class SampleSummaryToLibSVM {
	public static void convert(String sampleSummaryFilename, String libSvmFilename) throws IOException, Exception {
		List<TopicCount> topicCounts = new ArrayList<>();
		try(SampleSummaryFileReader r = new SampleSummaryFileReader(sampleSummaryFilename);
				LibSVMFileWriter w = new LibSVMFileWriter(libSvmFilename)) {
			
			for(DocTopicCounts dtc : r) {
				topicCounts.clear();
				while(dtc.hasNext()) {
					int topic = dtc.nextInt();
					int count = dtc.nextDocTopicCount();
					topicCounts.add(new TopicCount(topic, count));
				}
				Collections.sort(topicCounts, TopicCount.cmpTopic);
				
				w.startDocument(dtc.docClass());
				for(TopicCount tc : topicCounts) {
					w.featureValue(tc.topic, tc.count);
				}
				w.endDocument();
			}
		}
	}
}
