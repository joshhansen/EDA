package jhn.eda.io;

import java.io.IOException;

import jhn.eda.Paths;
import jhn.eda.tokentopics.DocTokenTopics;

public final class StateFiles {
	private StateFiles() {}
	
	public static StateFileReader read(String filename) throws IOException {
		if(filename.endsWith(jhn.eda.Paths.FAST_STATE_EXT)) {
			return new FastStateFileReader(filename);
		}
		if(filename.endsWith(jhn.eda.Paths.FASTER_STATE_EXT)) {
			return new FasterStateFileReader(filename);
		}
		throw new IllegalArgumentException("Don't know how to deal with files of this type");
	}
	
	public static StateFileWriter write(String filename, boolean includeClass) throws IOException {
		if(filename.endsWith(jhn.eda.Paths.FAST_STATE_EXT)) {
			return new FastStateFileWriter(filename, includeClass);
		}
		if(filename.endsWith(jhn.eda.Paths.FASTER_STATE_EXT)) {
			return new FasterStateFileWriter(filename, includeClass);
		}
		throw new IllegalArgumentException("Don't know how to deal with files of this type");
	}
	
	public static void print(String filename) throws IOException, Exception {
		try(StateFileReader r = read(filename)) {
			for(DocTokenTopics dtt : r) {
				System.out.print(dtt.docNum());
				System.out.print(' ');
				System.out.print(dtt.docSource());
				int topicNum;
				int tokenIdx;
				while(dtt.hasNext()) {
					topicNum = dtt.nextInt();
					tokenIdx = dtt.tokenIndex();
					System.out.print(' ');
					System.out.print(tokenIdx);
					System.out.print(':');
					System.out.print(topicNum);
				}
				System.out.println();
			}
		}
	}
	
	public static void main(String[] args) throws IOException, Exception {
		print(Paths.fasterStateFilename(jhn.Paths.outputDir("EDAValidation") + "/sotu_chunks/EDA2_1/runs/0", 2));
	}
}
