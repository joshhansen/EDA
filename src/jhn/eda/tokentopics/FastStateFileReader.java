package jhn.eda.tokentopics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

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
		try(FastStateFileReader dtt = new FastStateFileReader(jhn.eda.Paths.fastStateFilename(jhn.Paths.outputDir("EDAValidation")+"/sotu_chunks_subset1/eda_runs/0", 5))) {
			for(DocTokenTopics topics : dtt) {
				System.out.print(topics.docNum());
				System.out.print(": ");
				while(topics.hasNext()) {
					System.out.print(topics.nextTokenIndex());
					System.out.print(':');
					System.out.print(topics.nextInt());
					System.out.print(' ');
				}
				System.out.println();
			}
		}
	}

}
