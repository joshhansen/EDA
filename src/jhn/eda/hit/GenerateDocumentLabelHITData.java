package jhn.eda.hit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.LabelAlphabet;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import jhn.counts.Counter;
import jhn.counts.ints.IntIntCounter;
import jhn.counts.ints.IntIntIntRAMCounterMap;
import jhn.counts.ints.IntIntIntCounterMap;
import jhn.eda.Paths;
import jhn.eda.lucene.LuceneLabelAlphabet;
import jhn.idx.IntIndex;
import jhn.util.Util;

public class GenerateDocumentLabelHITData {
	public static IntIntIntCounterMap docTopicCounts(String sampleSummaryFilename) throws Exception {
		IntIntIntCounterMap counts = new IntIntIntRAMCounterMap();
		
		BufferedReader r = new BufferedReader(new FileReader(sampleSummaryFilename));
		
		int docNum;
		int lineNum = 0;
		String tmp = null;
		String[] parts;
		String[] subparts;
		while( (tmp=r.readLine()) != null) {
			if(!tmp.startsWith("#")) {
				parts = tmp.split("\\s+");
				
				docNum = Integer.parseInt(parts[1]);
				
				for(int i = 3; i < parts.length; i++) {
					subparts = parts[i].split(":");
					counts.set(docNum, Integer.parseInt(subparts[0]), Integer.parseInt(subparts[1]));
				}
				
				lineNum++;
				System.out.print('.');
				if(lineNum > 0 && lineNum % 120 == 0) {
					System.out.println(lineNum);
				}
			}
		}
		r.close();
		
		return counts;
	}
	
	private static Int2ObjectMap<String> docSources(String fastStateFilename) throws Exception {
		Int2ObjectMap<String> sources = new Int2ObjectOpenHashMap<String>();
		
		BufferedReader r = new BufferedReader(new FileReader(fastStateFilename));
		int docNum;
		String tmp = null;
		while( (tmp=r.readLine()) != null) {
			if(!tmp.startsWith("#")) {
				String[] parts = tmp.split("\\s+");
				
				docNum = Integer.parseInt(parts[0]);
				
				sources.put(docNum, parts[2]);
			}
		}
		
		return sources;
	}
	
	private static final Comparator<Int2ObjectMap.Entry<?>> cmp = new Comparator<Int2ObjectMap.Entry<?>>(){
		@Override
		public int compare(Entry<?> o1, Entry<?> o2) {
			return Util.compareInts(o1.getIntKey(), o2.getIntKey());
		}
	};
	
	private static void generate(String fastStateFilename, String topicWordIdxDir, String topicMappingFilename, String outputFilename, int topNlabels) throws Exception {
		LabelAlphabet labels = new LuceneLabelAlphabet(topicWordIdx);
		
		System.out.print("Deserializing topic mapping...");
		IntIndex topicMapping = (IntIndex) Util.deserialize(topicMappingFilename);
		System.out.println("done.");
		
		System.out.print("Counting topics...");
		IntIntIntCounterMap docTopicCounts = docTopicCounts(fastStateFilename);
		System.out.println("done.");
		
		System.out.print("Mapping document sources...");
		Int2ObjectMap<String> sources = docSources(fastStateFilename);
		IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(topicWordIdxDir)));
		System.out.println("done.");
		
		PrintStream w = new PrintStream(new FileOutputStream(outputFilename));
//		w.println("docnum,source,topic1num,topic1globalnum,\"topic1label\",...,topicNnum,topicNglobalnum,\"topicNlabel\"");
		w.println("docnum,source,topic1label,topic2label,topic3label,topic4label,topic5label,topic6label,topic7label,topic8label,topic9label,topic10label");
		
		int topicNum;
		int globalTopicNum;
		String label;
		
		@SuppressWarnings("unchecked")
		Int2ObjectMap.Entry<Counter<Integer,Integer>>[] entries = docTopicCounts.int2ObjectEntrySet().toArray(new Int2ObjectMap.Entry[0]);
		Arrays.sort(entries, cmp);
		
		for(Int2ObjectMap.Entry<Counter<Integer,Integer>> entry : entries) {
			w.print(entry.getIntKey());
			w.print(',');
			w.print(sources.get(entry.getIntKey()));
			
			for(Int2IntMap.Entry count : ((IntIntCounter)entry.getValue()).fastTopN(topNlabels)) {
				topicNum = count.getIntKey();
				globalTopicNum = topicMapping.objectAtI(topicNum);
				label = labels.lookupObject(globalTopicNum).toString();
				
//				w.print(',');
//				w.print(topicNum);
//				w.print(',');
//				w.print(globalTopicNum);
				w.print(",\"");
				w.print(label);
				w.print("\"");
			}
			w.println();
		}
		
		w.close();
	}
	
	public static void main(String[] args) throws Exception {
		final int minCount = 2;
		final String topicWordIdxName = "wp_lucene4";
		final String datasetName = "reuters21578";// toy_dataset2 debates2012 sacred_texts state_of_the_union reuters21578
		final int run = 17;
		final int iteration = 95;
		final int topNlabels = 10;
		
		String fastStateFilename =    Paths.fastStateFilename(run, iteration);
		String topicWordIdxDir =      Paths.topicWordIndexDir("wp_lucene4");
		String topicMappingFilename = Paths.topicMappingFilename(topicWordIdxName, datasetName, minCount);
		String outputFilename =       Paths.documentLabelHitDataFilename(run, iteration);
		
		generate(fastStateFilename, topicWordIdxDir, topicMappingFilename, outputFilename, topNlabels);
	}
}
