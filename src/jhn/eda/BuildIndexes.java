package jhn.eda;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import cc.mallet.types.LabelAlphabet;

public class BuildIndexes {
	
	public static abstract class FileVisitor {
		public void pre() {}
		public abstract void visit(File file); 
		public void post() {}
	}
	
	private static class DirSearch {
		private Set<FileVisitor> visitors = new HashSet<FileVisitor>();
		
		public void addVisitor(FileVisitor vis) {
			visitors.add(vis);
		}
		
		public void search(File file) {
			System.out.println("----- Pre-Search -----");
			for(FileVisitor vis : visitors) vis.pre();
			_search(file);
			System.out.println("----- Post-Search -----");
			for(FileVisitor vis : visitors) vis.post();
		}
		
		private void _search(File file) {
			File[] files = file.listFiles();
			Arrays.sort(files);
			for(File sub : files) {
				if(sub.isDirectory())
					_search(sub);
				else
					visit(sub);
			}
		}
		
		protected void visit(File file) {
			for(FileVisitor vis : visitors) {
				vis.visit(file);
			}
		}
	}
	
	public static class CounterVisitor extends FileVisitor {
		private int count = 0;
		@Override
		public void visit(File file) {
			count++;
			if(count % 10000 == 0) System.out.println(count);
		}
	}
	
	public static class IndexerVisitor extends FileVisitor {
		private LabelAlphabet topics;
		private String outputFilename;
		public IndexerVisitor(String outputFilename) {
			this.outputFilename = outputFilename;
		}
		
		public void pre() {
			topics = new LabelAlphabet();
		}
		@Override
		public void visit(File file) {
			final String topicName = file.getName().replace(".json","");
			final int topicNum = topics.lookupIndex(topicName);
			if(topicNum % 10000 == 0 && topicNum > 0)
				System.out.println(topicName + " -> " + topicNum);
		}
		public void post() {
			FileOutputStream fos = null;
			ObjectOutputStream out = null;
			try {
				fos = new FileOutputStream(outputFilename);
				out = new ObjectOutputStream(fos);
				out.writeObject(topics);
				out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		final String rootDirName = "/home/jjfresh/Projects/eda_output/counts/articles/_/_";
		final File rootDir = new File(rootDirName);
		final String outputFilename = "/home/jjfresh/Projects/eda_output/articletitlesindex.ser";
		
		DirSearch search = new DirSearch();
		search.addVisitor(new IndexerVisitor(outputFilename));
		
		search.search(rootDir);
	}

}
