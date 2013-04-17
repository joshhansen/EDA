package jhn.eda.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.LabelAlphabet;

import jhn.ExtractorParams;
import jhn.eda.lucene.LuceneLabelAlphabet;
import jhn.eda.tokentopics.DocTokenTopics;
import jhn.idx.IntIndex;
import jhn.util.Util;

public class FastStateFileReader implements StateFileReader {
	private final boolean includesClass;
	private BufferedReader r;
	private int[] topics;
	private int docNum = -1;
	private String docSource;
	private int docClass;
	private int idx;
	private int offset;
	
	public FastStateFileReader(String filename) throws IOException {
		r = new BufferedReader(new FileReader(filename));
		includesClass = r.readLine().contains(" class ");
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
			idx = 0;
			
			docNum = Integer.parseInt(parts[idx++]);
			if(includesClass) {
				docClass = Integer.parseInt(parts[idx++]);
			}
			docSource = parts[idx++];
			
			offset = idx;//adjusts for size of row header
			
			topics = new int[parts.length - offset];
			for(; idx < parts.length; idx++) {
				topics[idx-offset] = Integer.parseInt(parts[idx]);
			}
			return true;
		} catch(IOException e) {
			return false;
		}
	}

	@Override
	public DocTokenTopics next() {
		return new DocTokenTopics(docNum, docSource, topics, docClass);
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
		ExtractorParams ep = new ExtractorParams()
			.topicWordIdxName("wp_lucene4")
			.datasetName("reuters21578_noblah2")
			.minCount(2);
	
		String topicMappingFilename = jhn.Paths.topicMappingFilename(ep);
		IntIndex topicMapping = (IntIndex) Util.deserialize(topicMappingFilename);
		String topicWordIdxDir = jhn.Paths.topicWordIndexDir(ep.topicWordIdxName);
		try(IndexReader topicWordIdx = IndexReader.open(FSDirectory.open(new File(topicWordIdxDir)))) {
			LabelAlphabet labels = new LuceneLabelAlphabet(topicWordIdx);
			
//			String fastStateFilename = jhn.eda.Paths.fastStateFilename(jhn.Paths.outputDir("EDA")+"/runs/46", 2);
			String fastStateFilename = "/home/josh/Projects/Output/EDAValidation/reuters21578_noblah2/EDA2/runs/0/fast_state/11.fast_state";
			try(StateFileReader dtt = new FastStateFileReader(fastStateFilename)) {
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
