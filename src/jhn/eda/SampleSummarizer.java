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

import jhn.counts.Counter;
import jhn.counts.IntIntCounter;
import jhn.counts.IntIntIntCounterMap;
import jhn.util.Util;

public class SampleSummarizer {
	private static final Comparator<Int2IntMap.Entry> cmp = new Comparator<Int2IntMap.Entry>(){
		@Override
		public int compare(Int2IntMap.Entry o1, Int2IntMap.Entry o2) {
			return Util.compareInts(o1.getIntKey(), o2.getIntKey());
		}
	};
	
	private static void summarize(String fastStateDir, String summaryFilename) throws IOException {
		System.out.println("Summarizing " + fastStateDir + " -> " + summaryFilename);
		Int2IntMap classes = new Int2IntOpenHashMap();
		
		IntIntIntCounterMap aggregateDocTopicCounts = new IntIntIntCounterMap();
		
		String fullFilename;
		BufferedReader r;
		String tmp;
		String[] parts;
		int docNum;
		int docClass;
		int topic;
		int i;
		
		for(String filename : new File(fastStateDir).list()) {
			System.out.println(filename);
			fullFilename = fastStateDir + "/" + filename;
			
			r = new BufferedReader(new FileReader(fullFilename));
			tmp = null;
			while( (tmp=r.readLine()) != null) {
				if(!tmp.startsWith("#")) {
					parts = tmp.split("\\s+");
					
					docNum = Integer.parseInt(parts[0]);
					docClass = Integer.parseInt(parts[1]);
					classes.put(docNum, docClass);
					
//					String source = parts[2];
					
					for(i = 3; i < parts.length; i++) {
						topic = Integer.parseInt(parts[i]);
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
				
				w.write(' ');
				w.write(String.valueOf(topic));
				w.write(':');
				w.write(String.valueOf(count.getIntValue()));
			}
			w.newLine();
		}
		
		w.close();
	}
	
	public static void main(String[] args) throws IOException {
		int run = 17;
		String runDir = System.getenv("HOME") + "/Projects/eda_output/runs/" + run;
		String summaryFilename = runDir + "/aggregate_state.txt";
		
		summarize(runDir+"/fast_state", summaryFilename);
	}
}
