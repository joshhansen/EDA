package jhn.eda;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

public class Indexer {
	public static class AbstractsProcessor {
		public static abstract class Visitor {
			public void pre() {}
			public abstract void visit(final String s);
			public void post() {}
			
		}
		
		private String triplesFilename;
		private Set<Visitor> wordVisitors = new HashSet<Visitor>();
		private Set<Visitor> labelVisitors = new HashSet<Visitor>();
		
		public AbstractsProcessor(String triplesFilename) {
			this.triplesFilename = triplesFilename;
		}
		
		public void process() {
			try {
				NxParser nxp = new NxParser(Util.smartInputStream(triplesFilename));
				for(Node[] ns : nxp) {
					if(ns.length != 3) throw new IllegalArgumentException();
					for (Node n: ns) {
						System.out.print(n.toN3());
						System.out.print(" ");
					}
					System.out.println(".");
					
					
					
				}
//				while (nxp.hasNext()) {
//					Node[] ns = nxp.next();
//					if(ns.length != 3) throw new IllegalArgumentException();
//					
//
//
//					if (ns.length == 3)
//					{
//						//Only Process Triples  
//						//Replace the print statements with whatever you want
//						for (Node n: ns) 
//						{
//							System.out.print(n.toN3());
//							System.out.print(" ");
//						}
//						System.out.println(".");
//					}
//				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		protected void visitWord(final String word) {
			for(Visitor v : wordVisitors) v.visit(word);
		}
		
		protected void visitLabel(final String label) {
			for(Visitor v : labelVisitors) v.visit(label);
		}
	}
	
	public static void main(String[] args) {
		final String abstractsFilename = "/home/jjfresh/Data/dbpedia.org/3.7/short_abstracts_en.nt.bz2";
		AbstractsProcessor ap = new AbstractsProcessor(abstractsFilename);
		ap.process();
	}
}
