package jhn.eda.topiccounts;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.FSDirectory;

import jhn.eda.Paths;
import jhn.wp.Fields;

public class TopicTotalsCounter {
	private IndexReader src;
	private TopicCounter counter;
	public TopicTotalsCounter(IndexReader src, TopicCounter counter) {
		this.src = src;
		this.counter = counter;
	}
	
	public void count() throws CorruptIndexException, IOException {
		TermFreqVector tfv;
		int totalCount;
		for(int docNum = 0; docNum < src.numDocs(); docNum++) {
			tfv = src.getTermFreqVector(docNum, Fields.text);
			totalCount = 0;
			if(tfv != null) {
				int[] termFreqs = tfv.getTermFrequencies();
				for(int i = 0; i < termFreqs.length; i++) {
					totalCount += termFreqs[i];
				}
			}
			counter.setTotalCount(docNum, totalCount);
			System.out.print(docNum);
			System.out.print(": ");
			System.out.println(totalCount);
		}
		src.close();
		counter.close();
	}
	
	
	public static void main(String[] args) throws CorruptIndexException, IOException {
		final String topicWordIdxDir = Paths.outputDir() + "/indices/topic_words/wp_lucene4";
		IndexReader src = IndexReader.open(FSDirectory.open(new File(topicWordIdxDir)));
//		final String destFilename = Paths.outputDir() + "/indices/topic_totals/topic_totals.sqlite3";
		final String destFilename = Paths.outputDir() + "/indices/topic_counts/topic_counts.ser";
//		TopicCounter dest = new SqliteTopicCounts(destFilename);
		TopicCounter dest = new MapTopicCounts(destFilename);
		TopicTotalsCounter ttc = new TopicTotalsCounter(src, dest);
		ttc.count();
		
	}
}
