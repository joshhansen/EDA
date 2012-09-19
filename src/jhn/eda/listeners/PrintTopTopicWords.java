package jhn.eda.listeners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import jhn.counts.Counter;
import jhn.counts.i.i.i.IntIntIntCounterMap;
import jhn.counts.i.i.i.IntIntIntRAMCounterMap;
import jhn.eda.Paths;

public class PrintTopTopicWords extends IntervalListener {
	private final String runDir;
	private final int numWords;
	public PrintTopTopicWords(int printInterval, String runDir, int numWords) {
		super(printInterval);
		this.runDir = runDir;
		this.numWords = numWords;
		
		File dir = new File(Paths.topTopicWordsDir(runDir));
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	private static final Comparator<Entry<Integer,Counter<Integer,Integer>>> counterCmp = new Comparator<Entry<Integer,Counter<Integer,Integer>>>(){
		@Override
		public int compare(Entry<Integer, Counter<Integer,Integer>> o1, Entry<Integer, Counter<Integer,Integer>> o2) {
			return o2.getValue().totalCount().compareTo(o1.getValue().totalCount());
		}
	};

	@Override
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		IntIntIntCounterMap topicWordCounts = new IntIntIntRAMCounterMap();
		
		for(int docNum = 0; docNum < eda.numDocs(); docNum++) {
			for(int i = 0; i < eda.docLength(docNum); i++) {
				topicWordCounts.inc(eda.topic(docNum, i), eda.token(docNum, i));
			}
		}
		
		try(PrintStream out = new PrintStream(new FileOutputStream(Paths.topTopicWordsFilename(runDir, iteration)))) {
			out.println("Topic words:");
			List<Entry<Integer,Counter<Integer,Integer>>> topicWordCounters = new ArrayList<>(topicWordCounts.entrySet());
			Collections.sort(topicWordCounters, counterCmp);
			
			for(Entry<Integer,Counter<Integer,Integer>> counterEntry : topicWordCounters.subList(0, Math.min(eda.numTopics(), topicWordCounters.size()))) {
				out.print("#");
				out.print(counterEntry.getKey());
				out.print(" \"");
				
				//FIXME
//				out.print(topicAlphabet.lookupObject(counterEntry.getKey()));
				out.print(counterEntry.getKey());
				
				out.print("\" [total=");
				out.print(counterEntry.getValue().totalCount());
				out.println("]:");
				out.print('\t');
				
				for(Entry<Integer,Integer> countEntry : counterEntry.getValue().topN(numWords)) {
					Integer typeIdx = countEntry.getKey();
					Integer typeCount = countEntry.getValue();
					
					//FIXME
//					String type = alphabet.lookupObject(typeIdx).toString();
//					log.print('\t');
//					out.print(type);
					out.print(typeIdx);
					
					out.print("[");
					out.print(typeCount);
					out.print("] ");
				}
				out.println();
				out.println();
			}
		}
	}
	
}
