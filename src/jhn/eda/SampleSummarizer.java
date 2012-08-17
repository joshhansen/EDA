package jhn.eda;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import jhn.counts.Counter;
import jhn.counts.i.i.IntIntCounter;
import jhn.counts.i.i.i.IntIntIntCounterMap;
import jhn.counts.i.i.i.IntIntIntRAMCounterMap;
import jhn.util.Util;

public class SampleSummarizer {
	private static final Comparator<Int2IntMap.Entry> cmp = new Comparator<Int2IntMap.Entry>(){
		@Override
		public int compare(Int2IntMap.Entry o1, Int2IntMap.Entry o2) {
			return Util.compareInts(o1.getIntKey(), o2.getIntKey());
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
	
	public static void summarize(String runDir, int lastN, int minCount, boolean classOnly) throws IOException {
		String fastStateDir = Paths.fastStateDir(runDir);
		String summaryFilename = Paths.sampleSummaryFilename(runDir, lastN, minCount);
		
		System.out.println("Summarizing " + fastStateDir + " -> " + summaryFilename);
		Int2IntMap classes = new Int2IntOpenHashMap();
		Int2ObjectMap<String> sources = new Int2ObjectOpenHashMap<>();
		
		IntIntIntCounterMap aggregateDocTopicCounts = new IntIntIntRAMCounterMap();
		
		String fullFilename;
		BufferedReader r;
		String tmp;
		String[] parts;
		int docNum;
		int docClass;
		int topic;
		int j;
		
		File[] files = new File(fastStateDir).listFiles();
		Arrays.sort(files, fileCmp);
		
		
		for(int i = Math.max(files.length - lastN, 0); i < files.length; i++) {
			fullFilename = files[i].getPath();
			System.out.println(files[i].getName());
			
			r = new BufferedReader(new FileReader(fullFilename));
			tmp = null;
			while( (tmp=r.readLine()) != null) {
				if(!tmp.startsWith("#")) {
					parts = tmp.split("\\s+");
					
					docNum = Integer.parseInt(parts[0]);
					docClass = Integer.parseInt(parts[1]);
					classes.put(docNum, docClass);
					sources.put(docNum, parts[2]);
					
					for(j = 3; j < parts.length; j++) {
						topic = Integer.parseInt(parts[j]);
						aggregateDocTopicCounts.inc(docNum, topic);
					}
					
				}
			}
			
			r.close();
		}
		
		fullFilename = null;
		r = null;
		tmp = null;
		parts = null;
		
		
		IntIntCounter counts;
		Int2IntMap.Entry[] entries;
		
		try(PrintStream w = new PrintStream(new FileOutputStream(summaryFilename))) {
			w.print("#class");
			if(!classOnly) {
				w.print(" docnum docsrc");
			}
			w.println(" topic1id:topic1count topic2id:topic2count ... topicNid:topicNcount");
			for(Int2ObjectMap.Entry<Counter<Integer, Integer>> entry : aggregateDocTopicCounts.int2ObjectEntrySet()) {
				docNum = entry.getIntKey();
				docClass = classes.get(docNum);
				w.print(docClass);
				if(!classOnly) {
					w.print(' ');
					w.print(docNum);
					w.print(' ');
					w.print(sources.get(docNum));
				}
				
				counts = (IntIntCounter) entry.getValue();
				entries = counts.int2IntEntrySet().toArray(new Int2IntMap.Entry[0]);
				
				Arrays.sort(entries, cmp);
				
				for(Int2IntMap.Entry count : entries) {
					topic = count.getIntKey();
					if(count.getIntValue() >= minCount) {
						w.print(' ');
						w.print(topic);
						w.print(':');
						w.print(count.getIntValue());
					}
				}
				w.println();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		final int lastN = 50;
		final int run = 17;
		final int minCount = 0;
		final String runDir = Paths.runDir(Paths.defaultRunsDir(), run);
		summarize(runDir, lastN, minCount, false);
	}
}
