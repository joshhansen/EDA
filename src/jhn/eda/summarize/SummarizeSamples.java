package jhn.eda.summarize;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import jhn.counts.Counter;
import jhn.counts.i.i.IntIntCounter;
import jhn.counts.i.i.i.IntIntIntCounterMap;
import jhn.eda.Paths;
import jhn.eda.io.FastStateFileReader;
import jhn.eda.io.FasterStateFileReader;
import jhn.eda.io.SampleSummaryFileWriter;
import jhn.eda.io.StateFileReader;
import jhn.eda.tokentopics.DocTokenTopics;
import jhn.util.Util;

public class SummarizeSamples {
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
	
//	public static void summarize(SampleSummarizer summarizer, String runDir, int burn, int length, int minCount) throws Exception {
//		summarize(summarizer, runDir, burn, length, minCount, false);
//	}
	
//	public static void summarize(SampleSummarizer summarizer, String runDir, int burn, int length, int minCount, boolean includeClass) throws Exception {
	public static void summarize(String runDir, SummaryParams sp) throws Exception {
		String fasterStateDir = Paths.fasterStateDir(runDir);
		File[] allFiles = new File(fasterStateDir).listFiles();
		Arrays.sort(allFiles, fileCmp);
//		final int startIter = sp.firstIter();
//		final int stopIter = sp.lastIter(allFiles.length);
		
		SampleSummarizer summarizer = sp.summarizerCls.newInstance();
		
		// +1 because iteration filenames are 1-indexed
		String summaryFilename = Paths.sampleSummaryFilename(runDir, sp);
		
		System.out.println("Summarizing " + fasterStateDir + " -> " + summaryFilename);
		Int2ObjectMap<String> sources = new Int2ObjectOpenHashMap<>();
		
		List<File> files = new ArrayList<>();
		for(int i = sp.firstIter; i < sp.lastIter; i++) {
			files.add(allFiles[i]);
		}
		IntIntIntCounterMap aggregateDocTopicCounts = summarizer.summarize(files.toArray(new File[0]), sources);
		
		Int2IntMap classes;
		if(sp.includeClass) {
			classes = new Int2IntOpenHashMap();
			try(StateFileReader stateFile = new FasterStateFileReader(files.get(0).getPath())) {
				for(DocTokenTopics dtt : stateFile) {
					classes.put(dtt.docNum(), dtt.docClass());
				}
			}
		} else {
			classes = null;
		}
		
		int docNum;
		int docClass;
		int topic;
		int count;
		IntIntCounter counts;
		Int2IntMap.Entry[] entries;
		String docSrc;
		
		try(SampleSummaryFileWriter w = new SampleSummaryFileWriter(summaryFilename, sp.includeClass)) {
			int i = 0;
			ObjectSet<Entry<Counter<Integer, Integer>>> docTopicEntries = aggregateDocTopicCounts.int2ObjectEntrySet();
			for(Int2ObjectMap.Entry<Counter<Integer, Integer>> entry : docTopicEntries) {
				docNum = entry.getIntKey();
				docSrc = sources.get(docNum);
				if(sp.includeClass) {
					docClass = classes.get(docNum);
					w.startDocument(docNum, docSrc, docClass);
				} else {
					w.startDocument(docNum, docSrc);
				}
				
				counts = (IntIntCounter) entry.getValue();
				entries = counts.int2IntEntrySet().toArray(new Int2IntMap.Entry[0]);
				
				Arrays.sort(entries, cmp);
				
				for(Int2IntMap.Entry countEntry : entries) {
					topic = countEntry.getIntKey();
					count = countEntry.getIntValue();
					if(count >= sp.minCount) {
						w.topicCount(topic, count);
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
	
	public static void main(String[] args) throws Exception {
		SummaryParams sp = new SummaryParams();
		sp.summarizerCls = Sum.class;
		sp.includeClass = true;
		sp.firstIter = 30;
		sp.lastIter = 50;
		sp.minCount = 2;
		sp.includeClass = false;
		
//		final int run = 67;
//		final String runDir = Paths.runDir(Paths.defaultRunsDir(), run);
		final String runDir = jhn.Paths.outputDir("EDAValidation") + "/toy_dataset4/EDA2_1/runs/0";
		
		summarize(runDir, sp);
	}
}
