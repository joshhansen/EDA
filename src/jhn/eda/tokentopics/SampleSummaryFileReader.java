package jhn.eda.tokentopics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.LabelAlphabet;

import jhn.eda.Paths;
import jhn.eda.lucene.LuceneLabelAlphabet;
import jhn.idx.IntIndex;
import jhn.util.Util;

public class SampleSummaryFileReader implements Iterator<DocTopicCounts>, Iterable<DocTopicCounts>, AutoCloseable {
	
	private final BufferedReader r;
	private String nextLine;
	
	public SampleSummaryFileReader(String sampleSummaryFilename) throws IOException {
		r = new BufferedReader(new FileReader(sampleSummaryFilename));
		getNextNonCommentLine();
	}
	
	private void getNextNonCommentLine() throws IOException {
		do {
			nextLine = r.readLine();
		} while(nextLine.startsWith("#"));
	}
	
	@Override
	public Iterator<DocTopicCounts> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return nextLine != null;
	}

	@Override
	public DocTopicCounts next() {
		DocTopicCounts dtc = new DocTopicCounts(nextLine);
		try {
			getNextNonCommentLine();
		} catch (IOException e) {
			e.printStackTrace();
			nextLine = null;
		}
		return dtc;
	}
	
	@Override
	public void close() throws Exception {
		r.close();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	public static void main(String[] args) throws IOException, Exception {
		final int minCount = 2;
		final int summaryMinCount = 0;
		final int lastN = 50;
		final int run = 46;
		final String datasetName = "reuters21578_noblah2";
		final String topicWordIdxName = "wp_lucene4";
		String topicMappingFilename = Paths.topicMappingFilename(topicWordIdxName, datasetName, minCount);
		System.out.println(topicMappingFilename);
		IntIndex topicMapping = (IntIndex) Util.deserialize(topicMappingFilename);
		String topicWordIdxDir = jhn.Paths.topicWordIndexDir(topicWordIdxName);
		try(IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(topicWordIdxDir)))) {
			LabelAlphabet labels = new LuceneLabelAlphabet(topicWordIdx);

			final String runDir = Paths.runDir(Paths.defaultRunsDir(), run);
			String sampleSummaryFilename = Paths.sampleSummaryFilename(runDir, lastN, summaryMinCount);
			
			try(SampleSummaryFileReader r = new SampleSummaryFileReader(sampleSummaryFilename)) {
				for(DocTopicCounts dtc : r) {
					int docNum = dtc.docNum();
					System.out.print(docNum);
					System.out.print(": ");
					while(dtc.hasNext()) {
						int topicNum = dtc.nextInt();
						int count = dtc.nextDocTopicCount();
						int globalTopic = topicMapping.objectAtI(topicNum);
						String label = labels.lookupObject(globalTopic).toString();
						System.out.print(topicNum);
						System.out.print("(");
						System.out.print(globalTopic);
						System.out.print("):");
						System.out.print(count);
						System.out.print(':');
						System.out.print(label);
						System.out.print(' ');
					}
				}
			}
		}
	}
}
