package jhn.wp;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jhn.eda.Util;
import jhn.wp.exceptions.SkipException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

public class AbstractsCounter extends CorpusCounter {
	private String triplesFilename;
	private static final Pattern subjectRgx = Pattern.compile("^http://dbpedia\\.org/resource/(.+)$");
	
	public AbstractsCounter(String triplesFilename) {
		this.triplesFilename = triplesFilename;
	}
	
	public void count() {
		beforeEverything();
		try {
			NxParser nxp = new NxParser(Util.smartInputStream(triplesFilename));
			for(Node[] ns : nxp) {
				beforeLabel();
				if(ns.length != 3) System.err.println("Not a triple");
				final Matcher m = subjectRgx.matcher(ns[0].toString());
				m.matches();
				final String label = m.group(1);
				
				try {
					visitLabel(label);
					
					final String abstrakt = StringEscapeUtils.unescapeHtml4(ns[2].toString());
					for(String word : tokenize(abstrakt))
						if(!isStopword(word))
							visitWord(word);
					afterLabel();
				} catch(SkipException e) {
					System.err.write('s');
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		afterEverything();
	}
}