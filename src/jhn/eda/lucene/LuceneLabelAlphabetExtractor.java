package jhn.eda.lucene;

import java.io.File;
import java.io.IOException;

import jhn.util.Util;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import cc.mallet.types.LabelAlphabet;

public class LuceneLabelAlphabetExtractor {
	public static void main(String[] args) throws IOException {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
		final String name = "wp_lucene3";
		final String luceneDir = outputDir + "/" + name;
		final String alphaFilename = outputDir + "/" + name + "_label_alphabet.ser";
		
		FSDirectory dir = FSDirectory.open(new File(luceneDir));
		IndexReader r = IndexReader.open(dir);
		LabelAlphabet la = new LabelAlphabet();
		
		for(int docNum = 0; docNum < r.numDocs(); docNum++) {
			Document doc = r.document(docNum);
			la.lookupIndex(doc.get("label"));
			if(docNum % 1000 == 0 && docNum > 0) {
				System.out.print('.');
				if(docNum % 120000 == 0) {
					System.out.print(docNum);
					System.out.println();
				}
			}
		}
		
		Util.serialize(la, alphaFilename);
	}
}
