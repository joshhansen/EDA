package jhn.eda.lucene;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.store.FSDirectory;

public class LuceneTest {
	public static void main(String[] args) throws CorruptIndexException, IOException {
		final String outputDir = System.getenv("HOME") + "/Projects/eda_output";
		final String luceneDir = outputDir + "/wp_lucene";
		
		FSDirectory dir = FSDirectory.open(new File(luceneDir));
		
		IndexReader r = IndexReader.open(dir);
	
		Term t = new Term("text", "slavery");
		
		TermDocs tds = r.termDocs(t);
		while(tds.next()) {
			Document d = r.document(tds.doc());
			System.out.println(d.get("label") + " -> " + tds.freq());
		}
	}
}
