package jhn.eda.listeners;

import it.unimi.dsi.fastutil.ints.Int2IntMap;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import jhn.counts.i.i.IntIntCounter;
import jhn.eda.Paths;
import jhn.io.LibSVMFileWriter;
import jhn.util.Util;

public class PrintReducedDocsLibSVM extends IntervalListener {
	private final String runDir;
	private final boolean normalize;
	
	public PrintReducedDocsLibSVM(int printInterval, String run) {
		this(printInterval, run, true);
	}
	
	public PrintReducedDocsLibSVM(int printInterval, String run, boolean normalize) {
		super(printInterval);
		this.runDir = run;
		this.normalize = normalize;
		
		File dir = new File(Paths.reducedDir(run));
		if(!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public static final Comparator<Int2IntMap.Entry> fastKeyCmp = new Comparator<Int2IntMap.Entry>(){
		@Override
		public int compare(Int2IntMap.Entry o1, Int2IntMap.Entry o2) {
			return Util.compareInts(o1.getIntKey(), o2.getIntKey());
		}
	};
	
	
	@Override
	protected void iterationEndedAtInterval(int iteration) throws Exception {
		try(LibSVMFileWriter out = new LibSVMFileWriter(Paths.libSvmReducedFilename(runDir, iteration, normalize))) {
			int classNum;
			IntIntCounter docTopicCounts;
			double docLength;
			for(int docNum = 0; docNum < eda.numDocs(); docNum++) {
				classNum = eda.allLabels().indexOf(eda.docLabel(docNum), false);
				docTopicCounts = eda.docTopicCounter(docNum);
				docLength = eda.docLength(docNum);
				
				out.startDocument(classNum);
				
				Int2IntMap.Entry[] entries = docTopicCounts.int2IntEntrySet().toArray(new Int2IntMap.Entry[0]);
				Arrays.sort(entries, fastKeyCmp);
				
				for(Int2IntMap.Entry entry : entries) {
					if(normalize) {
						out.featureValue(entry.getIntKey(), entry.getIntValue() / docLength);
					} else {
						out.featureValue(entry.getIntKey(), entry.getIntValue());
					}
				}
				out.endDocument();
			}
		}
	}
}
