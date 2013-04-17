package jhn.eda.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.LabelAlphabet;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import jhn.ExtractorParams;
import jhn.eda.lucene.LuceneLabelAlphabet;
import jhn.eda.tokentopics.DocTokenTopics;
import jhn.idx.IntIndex;
import jhn.util.Util;

public class FasterStateFileReader implements StateFileReader {
	private final boolean includesClass;
	private DataInputStream r;
	private IntList topics = new IntArrayList();
	private int docNum = -1;
	private String docSource;
	private int docClass;
	private int topic;
	
	public FasterStateFileReader(String filename) throws IOException {
		r = new DataInputStream(new FileInputStream(filename));
		includesClass = r.readBoolean();
	}
	
	@Override
	public Iterator<DocTokenTopics> iterator() {
		return this;
	}
	
	@Override
	public boolean hasNext() {
		try {
			docNum = r.readInt();
			docSource = r.readUTF();
			if(includesClass) {
				docClass = r.readInt();
			}
			
			topics.clear();
			while( (topic=r.readInt()) >= 0) {
				topics.add(topic);
			}
			return true;
		} catch(IOException e) {
			return false;
		}
	}

	@Override
	public DocTokenTopics next() {
		return new DocTokenTopics(docNum, docSource, topics.toIntArray(), docClass);
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
			
			try(StateFileReader dtt = new FasterStateFileReader(jhn.eda.Paths.fasterStateFilename(jhn.Paths.outputDir("EDAValidation")+"/reuters21578_noblah2/EDA1/runs/0", 1))) {
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
