package jhn.wp.articles.visitors.lucene;

import java.io.File;
import java.io.IOException;

import jhn.wp.exceptions.CountException;
import jhn.wp.visitors.Visitor;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class LuceneVisitor extends Visitor {
	private static class Indexer {
		private IndexWriter writer;
		public Indexer(String indexDir) throws IOException {
			// the boolean true parameter means to create a new index everytime, 
			// potentially overwriting any existing files there.
			FSDirectory dir = FSDirectory.open(new File(indexDir));

			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_35);

			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_35, analyzer);

			writer = new IndexWriter(dir, config);
		}
		
		public void index(String label, String text) throws CorruptIndexException, IOException {
			Document doc = new Document();
			doc.add(new Field("label", label, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			doc.add(new Field("text", text, Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
			writer.addDocument(doc);
		}
		
		public void close() {
			try {
				writer.close();
			} catch (CorruptIndexException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private final String luceneIndexDir;
	private Indexer indexer;
	public LuceneVisitor(String luceneIndexDir) {
		this.luceneIndexDir = luceneIndexDir;
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				super.run();
				afterEverything();
			}
		});
	}
	
	private String label = null;
	private StringBuilder text;
	
	@Override
	public void beforeEverything() throws Exception {
		super.beforeEverything();
		indexer = new Indexer(luceneIndexDir);
	}

	@Override
	public void beforeLabel() {
		super.beforeLabel();
		text = new StringBuilder();
	}

	@Override
	public void visitLabel(String label) throws CountException {
		super.visitLabel(label);
		this.label = label;
	}

	@Override
	public void visitWord(String word) {
		super.visitWord(word);
		boolean needsSpace = text.length() > 0;
		if(needsSpace) {
			text.append(' ');
		}
		text.append(word);
	}

	@Override
	public void afterLabel() throws Exception {
		indexer.index(label, text.toString());
		super.afterLabel();
	}

	@Override
	public void afterEverything() {
		indexer.close();
		super.afterEverything();
	}
	
	
}
