package jhn.eda;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import jhn.counts.Counter;
import jhn.counts.i.i.IntIntCounter;
import jhn.counts.i.i.i.IntIntIntCounterMap;
import jhn.counts.i.i.i.IntIntIntRAMCounterMap;
import jhn.eda.tokentopics.DocTokenTopics;
import jhn.eda.tokentopics.FastStateFileReader;
import jhn.eda.tokentopics.SampleSummaryFileWriter;
import jhn.util.Util;

public class SampleSummarizer {
	private static final Comparator<Int2IntMap.Entry> cmp = new Comparator<Int2IntMap.Entry>(){
		@Override
		public int compare(Int2IntMap.Entry o1, Int2IntMap.Entry o2) {
			return Util.compareInts(o2.getIntValue(), o1.getIntValue());
		}
	};
	
	private static final Comparator<File> fileCmp = new Comparator<File>(){
		private int fileNum(File f) {
			String[] parts = f.getName().split("[.]");
			return Integer.parseInt(parts[0]);
		}
		
		@Override
		public int compare(File o1, File o2) {
			return Util.compareInts(fileNum(o1), fileNum(o2));
		}
	};
	
	public static void summarize(String runDir, int burn, int length, int minCount) throws IOException {
		summarize(runDir, burn, length, minCount, false);
	}
	
	public static void summarize(String runDir, int burn, int length, int minCount, boolean includeClass) throws IOException {
		String fastStateDir = Paths.fastStateDir(runDir);
		File[] files = new File(fastStateDir).listFiles();
		Arrays.sort(files, fileCmp);
		final int startIter = burn;
		final int stopIter = Math.min(files.length, burn+length);
		
		// +1 because iteration filenames are 1-indexed
		String summaryFilename = Paths.sampleSummaryFilename(runDir, startIter+1, stopIter, minCount);
		
		System.out.println("Summarizing " + fastStateDir + " -> " + summaryFilename);
		Int2IntMap classes = includeClass ? new Int2IntOpenHashMap() : null;
		Int2ObjectMap<String> sources = new Int2ObjectOpenHashMap<>();
		
		IntIntIntCounterMap aggregateDocTopicCounts = new IntIntIntRAMCounterMap();
		
		String fullFilename;
		int docNum;
		int docClass;
		int topic;
		
		for(int i = startIter; i < stopIter; i++) {
			fullFilename = files[i].getPath();
			System.out.println(files[i].getName());
			
			try(FastStateFileReader stateFile = new FastStateFileReader(fullFilename)) {
				for(DocTokenTopics topics : stateFile) {
					docNum = topics.docNum();
					sources.put(docNum, topics.docSource());
					while(topics.hasNext()) {
						aggregateDocTopicCounts.inc(docNum, topics.nextInt());
					}
				}
			}
		}
		
		fullFilename = null;
		
		
		IntIntCounter counts;
		Int2IntMap.Entry[] entries;
		String docSrc;
		
		try(SampleSummaryFileWriter w = new SampleSummaryFileWriter(summaryFilename, includeClass)) {
			int i = 0;
			ObjectSet<Entry<Counter<Integer, Integer>>> docTopicEntries = aggregateDocTopicCounts.int2ObjectEntrySet();
			for(Int2ObjectMap.Entry<Counter<Integer, Integer>> entry : docTopicEntries) {
				docNum = entry.getIntKey();
				docSrc = sources.get(docNum);
				if(includeClass) {
					docClass = classes.get(docNum);
					w.startDocument(docNum, docSrc, docClass);
				} else {
					w.startDocument(docNum, docSrc);
				}
				
				counts = (IntIntCounter) entry.getValue();
				entries = counts.int2IntEntrySet().toArray(new Int2IntMap.Entry[0]);
				
				Arrays.sort(entries, cmp);
				
				for(Int2IntMap.Entry count : entries) {
					topic = count.getIntKey();
					if(count.getIntValue() >= minCount) {
						w.topicCount(topic, count.getIntValue());
					}
				}
				w.endDocument();
				
				if(i % 100 == 0) {
					System.out.println(i + " / " + docTopicEntries.size());
				}
				i++;
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		final int burn = 10;
		final int length = 500;
		final int run = 47;
		final int minCount = 5;
		final String runDir = Paths.runDir(Paths.defaultRunsDir(), run);
		summarize(runDir, burn, length, minCount);
	}
}
