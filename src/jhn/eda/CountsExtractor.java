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
import jhn.eda.typetopiccounts.ArrayTypeTopicCounts;
import jhn.eda.typetopiccounts.LuceneTypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCount;
import jhn.eda.typetopiccounts.TypeTopicCounts;
import jhn.eda.typetopiccounts.TypeTopicCountsException;
import jhn.util.IntIndex;
import jhn.util.Util;

public class CountsExtractor {
	private final int typeCount;
	private final TypeTopicCounts srcCounts;
	private final LabelAlphabet srcLabels;
	private final int minCount;
	private final String topicCountsFilename;
	private final String typeTopicCountsFilename;
	private final String destLabelAlphabetFilename;
	
	public CountsExtractor(int typeCount, TypeTopicCounts srcCounts, LabelAlphabet srcLabels, int minCount,
			String topicCountsFilename, String typeTopicCountsFilename, String destLabelAlphabetFilename) throws Exception {
		this.typeCount = typeCount;
		this.srcCounts = srcCounts;
		this.srcLabels = srcLabels;
		this.minCount = minCount;
		this.topicCountsFilename = topicCountsFilename;
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
	
	public void extract() throws TypeTopicCountsException {
		IntIndex newTopicNums = new IntIndex();
		
		Int2IntOpenHashMap topicCounts = new Int2IntOpenHashMap();
		
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
				if(typeIdx > 0) System.out.print("\n\n");
				System.out.print(typeIdx);
				System.out.print('/');
				System.out.println(typeCount);
			}
			ttcs = srcCounts.typeTopicCounts(typeIdx);
			
			while(ttcs.hasNext()) {
				total++;
				ttc = ttcs.next();
				if(ttc.count >= minCount) {
					topic = newTopicNums.indexOf(ttc.topic);
					
					currentTypeTopicCounts.add(topic);
					currentTypeTopicCounts.add(ttc.count);
					
					topicCounts.add(topic, ttc.count);
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
		
		System.out.println();
		System.out.println("Empty types: " + emptyTypes + " / " + typeCount);
		System.out.println("Uni-topic types: " + uniTopicTypes + " / " + typeCount);
		System.out.println("Skipped type-topics: " + skipped + " / " + total);
		
		System.out.println("Topics: " + topicCounts.size());
		System.out.println("Highest topic frequency: " + Collections.max(topicCounts.values()));
		
		
		topicCounts.trim();
		Util.serialize(new ArrayTopicCounts(topicCountsArr(topicCounts)), topicCountsFilename);
		topicCounts = null;
		
		
		Util.serialize(new ArrayTypeTopicCounts(typeTopicCounts), typeTopicCountsFilename);
		
		
		// Label alphabet
		LabelAlphabet destLabels = new LabelAlphabet();
		for(int topicIdx = 0; topicIdx < newTopicNums.size(); topicIdx++) {
			int oldTopicIdx = newTopicNums.objectAt(topicIdx);
			String label = srcLabels.lookupObject(oldTopicIdx).toString();
			destLabels.lookupIndex(label, true);
		}
		
		Util.serialize(destLabels, destLabelAlphabetFilename);
	}
	
	
	
	public static void main(String[] args) throws Exception {
		// Config
		int minCount = 2;
		String datasetName = "debates2012";
		String topicWordIdxName = "wp_lucene4";
		String datasetFilename = Paths.datasetFilename(datasetName);
		String topicWordIdxLuceneDir = Paths.topicWordIndexDir(topicWordIdxName);
		
		String suffix = "/" + topicWordIdxName + ":" + datasetName + "_min" + minCount + ".ser";
		String topicCountsFilename = Paths.topicCountsDir() + suffix;
		String typeTopicCountsFilename = Paths.typeTopicCountsDir() + suffix;
		
		String destLabelAlphabetFilename = Paths.labelAlphabetFilename(topicWordIdxName, datasetName, minCount);
		
		// Load
		InstanceList targetData = InstanceList.load(new File(datasetFilename));
		Alphabet typeAlphabet = (Alphabet) targetData.getAlphabet().clone();
		int typeCount = typeAlphabet.size();

		targetData = null;
		
		IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(topicWordIdxLuceneDir)));
		TypeTopicCounts srcCounts = new LuceneTypeTopicCounts(topicWordIdx, typeAlphabet);
		LabelAlphabet srcLabels = new LuceneLabelAlphabet(topicWordIdx);
		
		// Run
		CountsExtractor ce = new CountsExtractor(typeCount, srcCounts, srcLabels, minCount, topicCountsFilename,
				typeTopicCountsFilename, destLabelAlphabetFilename);
		ce.extract();
	}
}
