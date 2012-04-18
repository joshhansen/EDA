package jhn.eda.lucene;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map.Entry;

import jhn.util.Counter;
import jhn.wp.Fields;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

public class PrefixExtractor {
	public static void main(String[] args) throws IOException {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
		final String luceneDir = outputDir + "/wp_lucene";
		final String outfile = outputDir + "/prefix_counts.csv";
		
		FSDirectory dir = FSDirectory.open(new File(luceneDir));
		IndexReader r = IndexReader.open(dir);
		Counter<String> prefixes = new Counter<String>();
		
		for(int docNum = 0; docNum < r.numDocs(); docNum++) {
			Document doc = r.document(docNum);
			String[] parts = doc.get(Fields.label).split(":");
			if(parts.length > 1) {
				prefixes.inc(parts[0]);
			}
			if(docNum % 1000 == 0 && docNum > 0) {
				System.out.print('.');
				if(docNum % 120000 == 0) {
					System.out.print(docNum);
					System.out.println();
				}
			}
		}
		
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(outfile));
			Entry<String,Double>[] entries = prefixes.entries().toArray(new Entry[0]);
			Arrays.sort(entries, new Comparator<Entry<String,Double>>(){
				@Override
				public int compare(Entry<String,Double> o1, Entry<String,Double> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});
			for(Entry<String,Double> entry : entries) {
				w.write(entry.getKey());
				w.write(',');
				w.write(entry.getValue().toString());
				w.write('\n');
			}
			w.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
	}
}
