package jhn.eda.listeners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import cc.mallet.types.IDSorter;

import jhn.counts.i.i.IntIntCounter;
import jhn.eda.Paths;

public class PrintDocTopics extends IntervalListener {
//	private final int run;
	private final String runDir;
	private final double threshold;
	private final int max;
	
	public PrintDocTopics(int printInterval, String runDir) {
		this(printInterval, runDir, 0.01, 100);
	}
	
	public PrintDocTopics(int printInterval, String runDir, double threshold, int max) {
		super(printInterval);
		
		if(max < 1) throw new IllegalArgumentException("Max must be 1 or greater");
		
		this.runDir = runDir;
//		this.run = run;
		this.threshold = threshold;
		this.max = max;
		
		File dir = new File(Paths.docTopicsDir(runDir));
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}

	@Override
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		try(PrintStream out = new PrintStream(new FileOutputStream(Paths.docTopicsFilename(runDir, iteration)))) {
			out.print ("#doc source topic proportion ...\n");
			final int numTopics = eda.numTopics();

			IDSorter[] sortedTopics = new IDSorter[ numTopics ];
			for (int topic = 0; topic < numTopics; topic++) {
				// Initialize the sorters with dummy values
				sortedTopics[topic] = new IDSorter(topic, topic);
			}

			for (int docNum = 0; docNum < eda.numDocs(); docNum++) {
				out.print (docNum); out.print (' ');
				out.print(eda.docName(docNum));
				out.print (' ');

				IntIntCounter topicCounts = eda.docTopicCounts(docNum);

				// And normalize
				for (int topic = 0; topic < numTopics; topic++) {
					sortedTopics[topic].set(topic, (double) topicCounts.getCount(topic) / (double) eda.docLength(docNum));
				}
				
				Arrays.sort(sortedTopics);

				for (int i = 0; i < Math.min(max, numTopics); i++) {
					if (sortedTopics[i].getWeight() < threshold) { break; }
					
					out.print (sortedTopics[i].getID() + " " + 
							  sortedTopics[i].getWeight() + " ");
				}
				out.print (" \n");
			}
		}
	}
	
}
