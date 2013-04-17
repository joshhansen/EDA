package jhn.eda.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.LabelAlphabet;

import jhn.ExtractorParams;
import jhn.eda.Paths;
import jhn.eda.lucene.LuceneLabelAlphabet;
import jhn.eda.tokentopics.DocTopicCounts;
import jhn.idx.IntIndex;
import jhn.util.Util;

public class SampleSummaryFileReader implements Iterator<DocTopicCounts>, Iterable<DocTopicCounts>, AutoCloseable {
	
	private final BufferedReader r;
	private String nextLine;
	private boolean includesClass;
	
	public SampleSummaryFileReader(String sampleSummaryFilename) throws IOException {
		r = new BufferedReader(new FileReader(sampleSummaryFilename));
		includesClass = r.readLine().contains(" class ");
		getNextNonCommentLine();
	}
	
	private void getNextNonCommentLine() throws IOException {
		do {
			nextLine = r.readLine();
		} while(nextLine != null && nextLine.startsWith("#"));
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
		DocTopicCounts dtc = new DocTopicCounts(nextLine, includesClass);
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
		ExtractorParams ep = new ExtractorParams()
			.topicWordIdxName("wp_lucene4")
			.datasetName("reuters21578_noblah2")
			.minCount(2);
		
		final int summaryMinCount = 5;
		final int startIter = 11;
		final int stopIter = 149;
		final int run = 67;
		
		
		final String summarizer = "majority";
		String topicMappingFilename = jhn.Paths.topicMappingFilename(ep);
		System.out.println(topicMappingFilename);
		IntIndex topicMapping = (IntIndex) Util.deserialize(topicMappingFilename);
		String topicWordIdxDir = jhn.Paths.topicWordIndexDir(ep.topicWordIdxName);
		try(IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(topicWordIdxDir)))) {
			LabelAlphabet labels = new LuceneLabelAlphabet(topicWordIdx);

			final String runDir = Paths.runDir(Paths.defaultRunsDir(), run);
			String sampleSummaryFilename = Paths.sampleSummaryFilename(summarizer, runDir, startIter, stopIter, summaryMinCount);
			
			try(SampleSummaryFileReader r = new SampleSummaryFileReader(sampleSummaryFilename)) {
				for(DocTopicCounts dtc : r) {
					@SuppressWarnings("unused")
					int docNum = dtc.docNum();
					String[] parts = dtc.docSource().split("/");
					
					System.out.print(parts[parts.length-2]);
					System.out.print('/');
					System.out.print(parts[parts.length-1]);
//					System.out.print(docNum);
					System.out.print(":\t");
					int i = 0;
					
					while(dtc.hasNext() && i < 5) {
						int topicNum = dtc.nextInt();
						int count = dtc.nextDocTopicCount();
						int globalTopic = topicMapping.objectAtI(topicNum);
						String label = labels.lookupObject(globalTopic).toString();
//						System.out.print(topicNum);
//						System.out.print("(");
//						System.out.print(globalTopic);
//						System.out.print("):");
//						System.out.print(count);
//						System.out.print(':');
//						System.out.print(label);
//						System.out.print(' ');
						
						System.out.print(label);
						System.out.print(" [");
						System.out.print(count);
						System.out.print("] ");
						
						i++;
					}
					System.out.println();
				}
			}
		}
	}
}
