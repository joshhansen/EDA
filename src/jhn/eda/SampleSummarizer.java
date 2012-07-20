package jhn.eda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import jhn.counts.Counter;
import jhn.counts.ints.IntIntCounter;
import jhn.counts.ints.IntIntIntRAMCounterMap;
import jhn.counts.ints.IntIntIntCounterMap;
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
	
	private static void summarize(int run, int lastN, int minCount) throws IOException {
		String fastStateDir = Paths.fastStateDir(run);
		String summaryFilename = Paths.sampleSummaryFilename(run, lastN, minCount);
		
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
//		for(String filename : new File(fastStateDir).list()) {
			System.out.println(files[i].getName());
//			fullFilename = fastStateDir + "/" + filename;
			
			r = new BufferedReader(new FileReader(fullFilename));
			tmp = null;
			while( (tmp=r.readLine()) != null) {
				if(!tmp.startsWith("#")) {
					parts = tmp.split("\\s+");
					
					docNum = Integer.parseInt(parts[0]);
					docClass = Integer.parseInt(parts[1]);
					classes.put(docNum, docClass);
					
//					String source = parts[2];
					
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
		
		BufferedWriter w = new BufferedWriter(new FileWriter(summaryFilename));
		w.write("#class topic1id:topic1count topic2id:topic2count ... topicNid:topicNcount\n");
		for(Int2ObjectMap.Entry<Counter<Integer, Integer>> entry : aggregateDocTopicCounts.int2ObjectEntrySet()) {
			docNum = entry.getIntKey();
			docClass = classes.get(docNum);
			
			w.write(String.valueOf(docClass));
			
			counts = (IntIntCounter) entry.getValue();
			entries = counts.int2IntEntrySet().toArray(new Int2IntMap.Entry[0]);
			
			Arrays.sort(entries, cmp);
			
			for(Int2IntMap.Entry count : entries) {
				topic = count.getIntKey();
				if(count.getIntValue() >= minCount) {
					w.write(' ');
					w.write(String.valueOf(topic));
					w.write(':');
					w.write(String.valueOf(count.getIntValue()));
				}
			}
			w.newLine();
		}
		
		w.close();
	}
	
	public static void main(String[] args) throws IOException {
		final int lastN = 10;
		final int run = 17;
		final int minCount = 2;
		summarize(run, lastN, minCount);
	}
}
