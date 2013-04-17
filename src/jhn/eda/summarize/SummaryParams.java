package jhn.eda.summarize;

public class SummaryParams {
		public Class<? extends SampleSummarizer> summarizerCls;
		public int firstIter;
		public int lastIter;
//		public int burn;
//		public int length;
		public int minCount;
		public boolean includeClass;
		
		public String summarizerName() {
			return summarizerCls.getSimpleName();
		}
		
//		public int firstIter() {
//			return burn;
//		}
//		
//		public int lastIter(int iterationCount) {
//			return Math.min(iterationCount, burn+length);
//		}
	}