package jhn.eda;

import java.io.File;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import jhn.eda.topiccounts.ArrayTopicCounts;
import jhn.eda.topiccounts.LuceneTopicCounts;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.topictypecounts.LuceneTopicTypeCounts;
import jhn.eda.topictypecounts.TopicTypeCount;
import jhn.eda.topictypecounts.TopicTypeCounts;
import jhn.eda.topictypecounts.TopicTypeCountsException;
import jhn.eda.typetopiccounts.ArrayTypeTopicCounts;
import jhn.eda.typetopiccounts.LuceneTypeTopicCounts;
import jhn.eda.typetopiccounts.TopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.idx.IntIndex;
import jhn.idx.IntRAMIndex;
import jhn.util.Config;
import jhn.util.Util;

public class CountsExtractor implements AutoCloseable {
	private static final boolean EXTRACT_FULL_COUNTS = false;
	private static final boolean EXTRACT_RESTRICTED_COUNTS = false;
	
	private TopicCounts srcTopicCounts;
	private TypeTopicCounts srcTypeTopicCounts;
	private TopicTypeCounts srcTopicTypeCounts;

	private final int typeCount;
	private final int minCount;
	
	private final String topicMappingFilename;
	private final String extractedPropsFilename;
	private final String topicCountsFilename;
	private final String restrictedTopicCountsFilename;
	private final String filteredTopicCountsFilename;
	private final String typeTopicCountsFilename;
	
	public CountsExtractor(TopicCounts srcTopicCounts, TypeTopicCounts srcTypeTopicCounts,
			TopicTypeCounts srcTopicTypeCounts, int typeCount, int minCount,
			String topicMappingFilename, String extractedPropsFilename,
			String topicCountsFilename, String restrictedTopicCountsFilename, String filteredTopicCountsFilename,
			String typeTopicCountsFilename) throws Exception {
		
		this.typeCount = typeCount;
		this.srcTopicCounts = srcTopicCounts;
		this.srcTypeTopicCounts = srcTypeTopicCounts;
		this.srcTopicTypeCounts = srcTopicTypeCounts;
		this.minCount = minCount;
		
		this.topicMappingFilename = topicMappingFilename;
		this.extractedPropsFilename = extractedPropsFilename;
		this.topicCountsFilename = topicCountsFilename;
		this.restrictedTopicCountsFilename = restrictedTopicCountsFilename;
		this.filteredTopicCountsFilename = filteredTopicCountsFilename;
		this.typeTopicCountsFilename = typeTopicCountsFilename;
	}
	
	private static int[] topicCountsArr(Int2IntMap topicCounts) {
		int[] countsArr = new int[topicCounts.size()];
		for(int i = 0; i < countsArr.length; i++) {
			countsArr[i] = topicCounts.get(i);
		}
		return countsArr;
	}
	
	public void extract() throws Exception {
		Config props = new Config();
		
		IntIndex newTopicNums = new IntRAMIndex();
		
		Int2IntOpenHashMap restrictedTopicCounts = EXTRACT_RESTRICTED_COUNTS ? new Int2IntOpenHashMap() : null;
		
		int[][] typeTopicCounts = new int[typeCount][];
		IntList currentTypeTopicCounts = new IntArrayList();
		
		int total = 0;
		int skipped = 0;
		int emptyTypes = 0;
		int uniTopicTypes = 0;
		
		TopicCount ttc;
		Iterator<TopicCount> ttcs;
		int topic;
		
		for(int typeIdx = 0; typeIdx < typeCount; typeIdx++) {
			if(typeIdx % 100 == 0) {
				System.out.print(typeIdx);
				System.out.print('/');
				System.out.println(typeCount);
			}
			ttcs = srcTypeTopicCounts.typeTopicCounts(typeIdx);
			
			while(ttcs.hasNext()) {
				total++;
				ttc = ttcs.next();
				if(ttc.count >= minCount) {
					topic = newTopicNums.indexOfI(ttc.topic);
					
					currentTypeTopicCounts.add(topic);
					currentTypeTopicCounts.add(ttc.count);
					
					if(EXTRACT_RESTRICTED_COUNTS) restrictedTopicCounts.add(topic, ttc.count);
				} else {
					skipped++;
				}
			}
			
			typeTopicCounts[typeIdx] = currentTypeTopicCounts.toIntArray();
			
			if(currentTypeTopicCounts.size() == 0) {
				emptyTypes++;
			} else if(currentTypeTopicCounts.size() == 1) {
				uniTopicTypes++;
			}
			currentTypeTopicCounts.clear();
		}
		srcTypeTopicCounts = null;
		
		System.out.println();
		System.out.println("Empty types: " + emptyTypes + " / " + typeCount);
		System.out.println("Uni-topic types: " + uniTopicTypes + " / " + typeCount);
		System.out.println("Skipped type-topics: " + skipped + " / " + total);
		
		System.out.println("Topics: " + newTopicNums.size());
//		System.out.println("Highest topic frequency: " + Collections.max(restrictedTopicCounts.values()));
		
		if(EXTRACT_RESTRICTED_COUNTS) {
		System.out.print("\nSerializing restricted topic counts...");
			restrictedTopicCounts.trim();
			Util.serialize(new ArrayTopicCounts(topicCountsArr(restrictedTopicCounts)), restrictedTopicCountsFilename);
			restrictedTopicCounts = null;
			System.out.println("done.");
		}
		
		System.out.print("Serializing type-topic counts...");
		Util.serialize(new ArrayTypeTopicCounts(typeTopicCounts), typeTopicCountsFilename);
		typeTopicCounts = null;
		System.out.println("done.");
		
		if(EXTRACT_FULL_COUNTS) {
			extractFullTopicCounts(newTopicNums);
		}
		
		extractFilteredTopicCounts(newTopicNums);
		
		System.out.print("Serializing topic index...");
		Util.serialize(newTopicNums, topicMappingFilename);
		System.out.println("done.");
		
		props.putInt(Options.NUM_TOPICS, newTopicNums.size());
		Util.serialize(props, extractedPropsFilename);
	}

	private void extractFullTopicCounts(IntIndex newTopicNums) throws Exception {
		System.out.print("Building full topic counts...");
		int[] topicCounts = new int[newTopicNums.size()];
		for(int topicNum = 0; topicNum < newTopicNums.size(); topicNum++) {
			topicCounts[topicNum] = srcTopicCounts.topicCount(topicNum);
		}
		srcTopicCounts = null;
		System.out.println("done.");
		
		System.out.print("Serializing full topic counts...");
		Util.serialize(new ArrayTopicCounts(topicCounts), topicCountsFilename);
		System.out.println("done.");
	}

	private void extractFilteredTopicCounts(IntIndex newTopicNums) throws TopicTypeCountsException {
		System.out.print("Building filtered topic counts...");
		int[] filteredTopicCounts = new int[newTopicNums.size()];
		
		TopicTypeCount topicTypeCount;
		Iterator<TopicTypeCount> topicTypeIt;
		
		for(int topicNum = 0; topicNum < newTopicNums.size(); topicNum++) {
			topicTypeIt = srcTopicTypeCounts.topicTypeCounts(topicNum);
			while(topicTypeIt.hasNext()) {
				topicTypeCount = topicTypeIt.next();
				if(topicTypeCount.count >= minCount) {
					filteredTopicCounts[topicNum] += topicTypeCount.count;
				}
			}
		}
		srcTopicTypeCounts = null;
		System.out.println("done.");
		
		System.out.print("Serializing filtered topic counts...");
		Util.serialize(new ArrayTopicCounts(filteredTopicCounts), filteredTopicCountsFilename);
		System.out.println("done.");
	}
	
	@Override
	public void close() throws Exception {
		Util.closeIfPossible(srcTopicCounts);
		Util.closeIfPossible(srcTypeTopicCounts);
		Util.closeIfPossible(srcTopicTypeCounts);
	}
	
	public static void main(String[] args) throws Exception {
		// Config
//		int minCount = 2;
		String datasetName = "reuters21578_noblah2";// toy_dataset2 debates2012 sacred_texts state_of_the_union reuters21578
//		String datasetName = "toy_dataset4";
		int minCount = 2;
		String topicWordIdxName = "wp_lucene4";
		System.out.println("Extracting " + datasetName);
		String datasetFilename = jhn.Paths.malletDatasetFilename(datasetName);
		String topicWordIdxLuceneDir = jhn.Paths.topicWordIndexDir(topicWordIdxName);
		
		String topicMappingFilename =              jhn.Paths.topicMappingFilename(topicWordIdxName, datasetName, minCount);
		String propsFilename =                 jhn.Paths.propsFilename(topicWordIdxName, datasetName, minCount);
		String topicCountsFilename =           jhn.Paths.topicCountsFilename(topicWordIdxName, datasetName, minCount);
		String restrictedTopicCountsFilename = jhn.Paths.restrictedTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		String filteredTopicCountsFilename =   jhn.Paths.filteredTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		String typeTopicCountsFilename =       jhn.Paths.typeTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		
		// Load
		InstanceList targetData = InstanceList.load(new File(datasetFilename));
		Alphabet typeAlphabet = (Alphabet) targetData.getAlphabet().clone();
		final int typeCount = typeAlphabet.size();

		targetData = null;
		
		IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(topicWordIdxLuceneDir)));
		TopicCounts srcTopicCounts = new LuceneTopicCounts(topicWordIdx);
		TypeTopicCounts srcTypeTopicCounts = new LuceneTypeTopicCounts(topicWordIdx, typeAlphabet);
		TopicTypeCounts srcTopicTypeCounts = new LuceneTopicTypeCounts(topicWordIdx, typeAlphabet);
		
		// Run
		try(CountsExtractor ce = new CountsExtractor(srcTopicCounts, srcTypeTopicCounts, srcTopicTypeCounts,
				typeCount, minCount, topicMappingFilename, propsFilename, topicCountsFilename, restrictedTopicCountsFilename,
				filteredTopicCountsFilename, typeTopicCountsFilename)) {
			
			ce.extract();
		}
	}
}
