package jhn.eda;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import jhn.eda.lucene.LuceneLabelAlphabet;
import jhn.eda.topiccounts.ArrayTopicCounts;
import jhn.eda.topiccounts.LuceneTopicCounts;
import jhn.eda.topiccounts.TopicCounts;
import jhn.eda.typetopiccounts.ArrayTypeTopicCounts;
import jhn.eda.typetopiccounts.LuceneTypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.util.IntIndex;
import jhn.util.Util;

public class CountsExtractor {
	private TopicCounts srcTopicCounts;
	private TypeTopicCounts srcTypeTopicCounts;
	private LabelAlphabet srcLabels;
	private final int typeCount;
	private final int minCount;
	private final String topicCountsFilename;
	private final String restrictedTopicCountsFilename;
	private final String typeTopicCountsFilename;
	private final String destLabelAlphabetFilename;
	
	public CountsExtractor(TopicCounts srcTopicCounts, TypeTopicCounts srcTypeTopicCounts, LabelAlphabet srcLabels, int typeCount, int minCount,
			String topicCountsFilename, String restrictedTopicCountsFilename, String typeTopicCountsFilename, String destLabelAlphabetFilename) throws Exception {
		this.typeCount = typeCount;
		this.srcTopicCounts = srcTopicCounts;
		this.srcTypeTopicCounts = srcTypeTopicCounts;
		this.srcLabels = srcLabels;
		this.minCount = minCount;
		this.topicCountsFilename = topicCountsFilename;
		this.restrictedTopicCountsFilename = restrictedTopicCountsFilename;
		this.typeTopicCountsFilename = typeTopicCountsFilename;
		this.destLabelAlphabetFilename = destLabelAlphabetFilename;
	}
	
	private int[] topicCountsArr(Int2IntMap topicCounts) {
		int[] countsArr = new int[topicCounts.size()];
		for(int i = 0; i < countsArr.length; i++) {
			countsArr[i] = topicCounts.get(i);
		}
		return countsArr;
	}
	
	public void extract() throws Exception {
		IntIndex newTopicNums = new IntIndex();
		
		Int2IntOpenHashMap restrictedTopicCounts = new Int2IntOpenHashMap();
		
		int[][] typeTopicCounts = new int[typeCount][];
		IntList currentTypeTopicCounts = new IntArrayList();
		
		int total = 0;
		int skipped = 0;
		int emptyTypes = 0;
		int uniTopicTypes = 0;
		
		TypeTopicCount ttc;
		Iterator<TypeTopicCount> ttcs;
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
					topic = newTopicNums.indexOf(ttc.topic);
					
					currentTypeTopicCounts.add(topic);
					currentTypeTopicCounts.add(ttc.count);
					
					restrictedTopicCounts.add(topic, ttc.count);
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
		System.out.println("Highest topic frequency: " + Collections.max(restrictedTopicCounts.values()));
		
		System.out.print("\nSerializing restricted topic counts...");
		restrictedTopicCounts.trim();
		Util.serialize(new ArrayTopicCounts(topicCountsArr(restrictedTopicCounts)), restrictedTopicCountsFilename);
		restrictedTopicCounts = null;
		System.out.println("done.");
		
		System.out.print("Serializing type-topic counts...");
		Util.serialize(new ArrayTypeTopicCounts(typeTopicCounts), typeTopicCountsFilename);
		typeTopicCounts = null;
		System.out.println("done.");
		
		
		// Label alphabet
		System.out.print("Building label alphabet...");
		LabelAlphabet destLabels = new LabelAlphabet();
		for(int topicIdx = 0; topicIdx < newTopicNums.size(); topicIdx++) {
			int oldTopicIdx = newTopicNums.objectAt(topicIdx);
			String label = srcLabels.lookupObject(oldTopicIdx).toString();
			destLabels.lookupIndex(label, true);
		}
		srcLabels = null;
		System.out.println("done.");
		
		System.out.print("Serializing label alphabet...");
		Util.serialize(destLabels, destLabelAlphabetFilename);
		destLabels = null;
		System.out.println("done.");
		
		System.out.print("Building topic counts...");
		int[] topicCounts = new int[newTopicNums.size()];
		for(int topicNum = 0; topicNum < newTopicNums.size(); topicNum++) {
			topicCounts[topicNum] = srcTopicCounts.topicCount(topicNum);
		}
		srcTopicCounts = null;
		System.out.println("done.");
		
		System.out.print("Serializing topic counts...");
		Util.serialize(new ArrayTopicCounts(topicCounts), topicCountsFilename);
		System.out.println("done.");
	}
	
	
	
	public static void main(String[] args) throws Exception {
		// Config
		int minCount = 2;
		String datasetName = "state_of_the_union";// debates2012 toy_dataset2
		String topicWordIdxName = "wp_lucene4";
		String datasetFilename = Paths.datasetFilename(datasetName);
		String topicWordIdxLuceneDir = Paths.topicWordIndexDir(topicWordIdxName);
		
		String topicCountsFilename =           Paths.topicCountsFilename(topicWordIdxName, datasetName, minCount);
		String restrictedTopicCountsFilename = Paths.restrictedTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		String typeTopicCountsFilename =       Paths.typeTopicCountsFilename(topicWordIdxName, datasetName, minCount);
		String destLabelAlphabetFilename =     Paths.labelAlphabetFilename(topicWordIdxName, datasetName, minCount);
		
		// Load
		InstanceList targetData = InstanceList.load(new File(datasetFilename));
		Alphabet typeAlphabet = (Alphabet) targetData.getAlphabet().clone();
		final int typeCount = typeAlphabet.size();

		targetData = null;
		
		IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(topicWordIdxLuceneDir)));
		TopicCounts srcTopicCounts = new LuceneTopicCounts(topicWordIdx);
		TypeTopicCounts srcCounts = new LuceneTypeTopicCounts(topicWordIdx, typeAlphabet);
		LabelAlphabet srcLabels = new LuceneLabelAlphabet(topicWordIdx);
		
		// Run
		CountsExtractor ce = new CountsExtractor(srcTopicCounts, srcCounts, srcLabels, typeCount, minCount,
				topicCountsFilename, restrictedTopicCountsFilename, typeTopicCountsFilename, destLabelAlphabetFilename);
		ce.extract();
		
		topicWordIdx.close();
	}
}
