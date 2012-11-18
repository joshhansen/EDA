package jhn.eda.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.LabelAlphabet;

import jhn.eda.Paths;
import jhn.eda.lucene.LuceneLabelAlphabet;
import jhn.eda.tokentopics.DocTokenTopics;
import jhn.idx.IntIndex;
import jhn.util.Util;

public class FastStateFileReader implements Iterable<DocTokenTopics>, Iterator<DocTokenTopics>, AutoCloseable {
	private BufferedReader r;
	private int[] topics;
	private int docNum = -1;
	private String docSource;
	
	public FastStateFileReader(String filename) throws FileNotFoundException {
		r = new BufferedReader(new FileReader(filename));
	}
	
	@Override
	public Iterator<DocTokenTopics> iterator() {
		return this;
	}

	
	@Override
	public boolean hasNext() {
		try {
			String line;
			while(true) {
				line = r.readLine();
				if(line == null) {
					return false;
				}
				if(!line.startsWith("#")) {
					break;
				}
			}
			
			String[] parts = line.split("\\s+");
			docNum = Integer.parseInt(parts[0]);
			docSource = parts[1];
			
			topics = new int[parts.length - 2];
			for(int j = 2; j < parts.length; j++) {
				topics[j-2] = Integer.parseInt(parts[j]);
			}
			return true;
		} catch(IOException e) {
			return false;
		}
	}

	@Override
	public DocTokenTopics next() {
		return new DocTokenTopics(docNum, docSource, topics);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void close() throws IOException {
		r.close();
	}
	
	public static void main(String[] args) throws Exception {
		final int minCount = 2;
		final String datasetName = "reuters21578_noblah2";
		final String topicWordIdxName = "wp_lucene4";
		String topicMappingFilename = Paths.topicMappingFilename(topicWordIdxName, datasetName, minCount);
		IntIndex topicMapping = (IntIndex) Util.deserialize(topicMappingFilename);
		String topicWordIdxDir = jhn.Paths.topicWordIndexDir(topicWordIdxName);
		try(IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(topicWordIdxDir)))) {
			LabelAlphabet labels = new LuceneLabelAlphabet(topicWordIdx);
			
			try(FastStateFileReader dtt = new FastStateFileReader(jhn.eda.Paths.fastStateFilename(jhn.Paths.outputDir("EDA")+"/runs/46", 2))) {
				for(DocTokenTopics topics : dtt) {
					System.out.print(topics.docNum());
					System.out.print(' ');
					System.out.print(topics.docSource());
					System.out.print(": ");
					while(topics.hasNext()) {
						int tokenIdx = topics.nextTokenIndex();
						int topic = topics.nextInt();
						int globalTopic = topicMapping.objectAtI(topic);
						String label = labels.lookupObject(globalTopic).toString();
						System.out.print(tokenIdx);
						System.out.print(':');
						System.out.print(topic);
						System.out.print(" (");
						System.out.print(globalTopic);
						System.out.print(") ");
						System.out.print(label);
						System.out.print(' ');
					}
					System.out.println();
				}
			}
		}
	}
}
