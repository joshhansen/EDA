package jhn.eda.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jhn.eda.Util;

import org.apache.commons.lang3.StringEscapeUtils;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

public class AbstractsProcessor {
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
	
	private static final Pattern tokenSplitRgx = Pattern.compile("[^a-z]");
	protected String[] tokenize(final String abstrakt) {
		return tokenSplitRgx.split(abstrakt.toLowerCase());
	}
	
	public void addVisitor(Visitor v) { visitors.add(v); }
	public List<Visitor> visitors() {
		return Collections.unmodifiableList(visitors);
	}
}