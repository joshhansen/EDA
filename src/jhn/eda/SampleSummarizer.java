package jhn.eda;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import jhn.counts.Counter;
import jhn.counts.IntIntIntCounterMap;

public class SampleSummarizer {
	private static void summarize(String fastStateDir, String summaryFilename) throws IOException {
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
		
		BufferedWriter w = new BufferedWriter(new FileWriter(summaryFilename));
		w.write("#class topic1id:topic1count topic2id:topic2count ... topicNid:topicNcount\n");
		for(Int2ObjectMap.Entry<Counter<Integer, Integer>> entry : aggregateDocTopicCounts.int2ObjectEntrySet()) {
			docNum = entry.getIntKey();
			docClass = classes.get(docNum);
			
			w.write(String.valueOf(docClass));
			
			for(Entry<Integer,Integer> count : entry.getValue().entries()) {
				topic = count.getKey().intValue();
				
				w.write(' ');
				w.write(String.valueOf(topic));
				w.write(':');
				w.write(count.getValue().toString());
				
			}
			w.newLine();
		}
		
		w.close();
	}
	
	
	public static void main(String[] args) throws IOException {
		int run = 12;
		String runDir = System.getenv("HOME") + "/Projects/eda_output/runs/" + run;
		String summaryFilename = runDir + "/aggregate_state.txt";
		
		summarize(runDir+"/fast_state", summaryFilename);
	}
}
