package jhn.eda;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import cc.mallet.types.Alphabet;
import cc.mallet.types.LabelAlphabet;

public class Indexer {
	public static class Visitor {
		public void beforeEverything() {}
		public void beforeLabel() {}
		public void visitLabel(final String label) {}
		public void visitWord(final String word) {}
		public void afterLabel() {}
		public void afterEverything() {}
	}
	
	public static class AbstractsProcessor {
		private String triplesFilename;
		private Set<String> stopwords;
		private List<Visitor> visitors = new ArrayList<Visitor>();
		private static final Pattern subjectRgx = Pattern.compile("^http://dbpedia\\.org/resource/(.+)$");
		
		public AbstractsProcessor(String triplesFilename, Set<String> stopwords) {
			this.triplesFilename = triplesFilename;
			this.stopwords = stopwords;
		}
		
		public void process() {
			beforeEverything();
			try {
				NxParser nxp = new NxParser(Util.smartInputStream(triplesFilename));
				for(Node[] ns : nxp) {
					beforeLabel();
					if(ns.length != 3) System.err.println("Not a triple");
					final Matcher m = subjectRgx.matcher(ns[0].toString());
					m.matches();
					final String label = m.group(1);
					
					visitLabel(label);
					
					final String abstrakt = StringEscapeUtils.unescapeHtml4(ns[2].toString());
					for(String word : tokenize(abstrakt))
						if(!stopwords.contains(word))
							visitWord(word);
					afterLabel();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			afterEverything();
		}
		
		protected void beforeEverything() { for(Visitor v : visitors) v.beforeEverything(); }
		protected void beforeLabel() { for(Visitor v : visitors) v.beforeLabel(); }
		protected void visitLabel(final String label) { for(Visitor v : visitors) v.visitLabel(label); }
		protected void visitWord(final String word) { for(Visitor v : visitors) v.visitWord(word); }
		protected void afterLabel() { for(Visitor v : visitors) v.afterLabel(); }
		protected void afterEverything() { for(Visitor v : visitors) v.afterEverything(); }
		
		private static final Pattern tokenSplitRgx = Pattern.compile("[^a-z\\-]");
		protected String[] tokenize(final String abstrakt) {
			return tokenSplitRgx.split(abstrakt.toLowerCase());
		}
	}
	
	private static class IndexingVisitor extends Visitor {
		protected Alphabet index;
		private String outputFilename;
		
		public void afterEverything() {
			FileOutputStream fos = null;
			ObjectOutputStream out = null;
			try {
				fos = new FileOutputStream(outputFilename);
				out = new ObjectOutputStream(fos);
				out.writeObject(index);
				out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private static class LabelIndexingVisitor extends IndexingVisitor {
		public void beforeEverything() {
			index = new LabelAlphabet();
		}
		
		public void visitLabel(String label) {
			index.lookupIndex(label, true);
		}
	}
	
	private static class WordIndexingVisitor extends IndexingVisitor {
		public void beforeEverything() {
			index = new Alphabet();
		}
		
		public void visitWord(String word) {
			index.lookupIndex(word, true);
		}
	}
	
	private static class AbstractCountingVisitor extends Visitor {
		private final String outputDir;
		private final Alphabet wordIdx;
		private Map<Integer,Integer> counts;
		private String label;
		
		public AbstractCountingVisitor(String outputDir, Alphabet wordIdx) {
			this.outputDir = outputDir;
			this.wordIdx = wordIdx;
		}

		public void beforeLabel() {
			counts = new HashMap<Integer,Integer>();
		}
		
		public void visitLabel(String label) {
			this.label = label;
		}
		
		public void visitWord(String word) {
			Integer idx = Integer.valueOf(wordIdx.lookupIndex(word));
			Integer count = counts.get(idx);
			
			if(count == null) count = 1;
			else count++;
			
			counts.put(idx, count);
		}
		
		public void afterLabel() {
			
		}
	}
	
	public static void main(String[] args) {
		final Set<String> stopwords = new HashSet<String>();
		for(String stopword : Util.stopwords) stopwords.add(stopword);
		
		
		final String abstractsFilename = "/home/jjfresh/Data/dbpedia.org/3.7/short_abstracts_en.nt.bz2";
		AbstractsProcessor ap = new AbstractsProcessor(abstractsFilename, stopwords);
		ap.process();
	}
}
