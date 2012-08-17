package jhn.eda.listeners;

import it.unimi.dsi.fastutil.ints.Int2IntMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import jhn.counts.i.i.IntIntCounter;
import jhn.counts.i.i.i.IntIntIntCounterMap;
import jhn.counts.i.i.i.IntIntIntRAMCounterMap;
import jhn.eda.Paths;

public class PrintTopDocTopics extends IntervalListener {
	private final String runDir;
	private final int numWords;
	public PrintTopDocTopics(int printInterval, String run, int numWords) {
		super(printInterval);
		this.runDir = run;
		this.numWords = numWords;
		
		File dir = new File(Paths.topDocTopicsDir(run));
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}

	@Override
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		IntIntIntCounterMap docTopicCounts = new IntIntIntRAMCounterMap();
		for(int docNum = 0; docNum < eda.numDocs(); docNum++) {
			for(int i = 0; i < eda.docLength(docNum); i++) {
				docTopicCounts.inc(docNum, eda.topic(docNum, i));
			}
		}
		
		try(PrintStream out = new PrintStream(new FileOutputStream(Paths.topDocTopicsFilename(runDir, iteration)))) {
			out.println("Documents topics:");
			
			for(int docNum = 0; docNum < eda.numDocs(); docNum++) {
				IntIntCounter counter = docTopicCounts.getCounter(docNum);
				
				out.print(eda.docName(docNum));
				out.print(" [total=");
				out.print(counter.totalCount());
				out.println("]:");
				
				for(Int2IntMap.Entry countEntry : counter.fastTopN(numWords)) {
					int topicIdx = countEntry.getIntKey();
					int typeCount = countEntry.getIntValue();
					out.print("\t");
					
					//FIXME
//					String topicLabel = topicAlphabet.lookupObject(topicIdx).toString();
//					out.print(topicLabel);
					out.print(topicIdx);
					out.print("[");
					out.print(typeCount);
					out.println("]");
				}
				out.println();
			}
		}
	}

}
