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
	
	private static class CountingVisitor extends Visitor {
		private int count;
		private final String name;
		
		public CountingVisitor(final String name) {
			this.name = name;
		}
		
		public void beforeEverything() {
			count = 0;
		}
		
		public void afterEverything() {
			System.out.println("Count '" + name + "' = " + count);
		}
		
		protected int increment() {
			return ++count;
		}
		
		protected int count() {
			return count;
		}
	}
	
	private static class LabelCountingVisitor extends CountingVisitor {
		public LabelCountingVisitor() {
			super("Labels");
		}
		public void visitLabel(String label) {
			int count = increment();
			if(count % 10000 == 0) System.out.println(count + " " + label);
		}
	}
	
	private static class WordCountingVisitor extends Visitor {
		private Set<String> words = new HashSet<String>();
		public WordCountingVisitor() {
		}
		public void visitWord(String word) {
			words.add(word);
		}
		
		public void afterEverything() {
			System.out.println("Word Types: " + words.size());
		}
	}
	
	private static class IndexingVisitor extends Visitor {
		protected Alphabet index;
		private final String outputFilename;
		
		public IndexingVisitor(String outputFilename) {
			this.outputFilename = outputFilename;
		}

		public void afterEverything() {
			Util.serialize(index, outputFilename);
		}
	}
	
	private static class LabelIndexingVisitor extends IndexingVisitor {
		public LabelIndexingVisitor(String outputFilename) {
			super(outputFilename);
		}

		public void beforeEverything() {
			index = new LabelAlphabet();
		}
		
		public void visitLabel(String label) {
			int idx = index.lookupIndex(label, true);
			if(idx % 10000 == 0 && idx > 0)
				System.out.println(idx + " " + label);
		}
	}
	
	private static class WordIndexingVisitor extends IndexingVisitor {
		public WordIndexingVisitor(String outputFilename) {
			super(outputFilename);
		}

		public void beforeEverything() {
			index = new Alphabet();
		}
		
		public void visitWord(String word) {
			index.lookupIndex(word, true);
		}
	}
	
//	private static class AbstractCountingVisitor extends Visitor {
//		private final String outputDir;
//		private final Alphabet wordIdx;
//		private Map<Integer,Integer> counts;
//		private String label;
//		
//		public AbstractCountingVisitor(String outputDir, Alphabet wordIdx) {
//			this.outputDir = outputDir;
//			this.wordIdx = wordIdx;
//		}
//
//		public void beforeLabel() {
//			counts = new HashMap<Integer,Integer>();
//		}
//		
//		public void visitLabel(String label) {
//			this.label = label;
//		}
//		
//		public void visitWord(String word) {
//			Integer idx = Integer.valueOf(wordIdx.lookupIndex(word));
//			Integer count = counts.get(idx);
//			
//			if(count == null) count = 1;
//			else count++;
//			
//			counts.put(idx, count);
//		}
//		
//		public void afterLabel() {
//			
//		}
//	}
	
	private static final int LABEL_COUNT = 3550567;
	private static final int WORD_TYPE_COUNT = 1978075;
	
	public static void main(String[] args) {
		final String outputDir = "/home/jjfresh/Projects/eda_output";
		final Set<String> stopwords = new HashSet<String>();
		for(String stopword : Util.stopwords) stopwords.add(stopword);
		
		
		final String abstractsFilename = "/home/jjfresh/Data/dbpedia.org/3.7/short_abstracts_en.nt.bz2";
		AbstractsProcessor ap = new AbstractsProcessor(abstractsFilename, stopwords);
		ap.visitors.add(new LabelIndexingVisitor(outputDir+"/labelAlphabet.ser"));
		ap.visitors.add(new WordIndexingVisitor(outputDir+"/alphabet.ser"));
//		ap.visitors.add(new LabelCountingVisitor());
//		ap.visitors.add(new WordCountingVisitor());
		ap.process();
	}
}
