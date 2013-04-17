package jhn.eda;

import jhn.eda.summarize.SummaryParams;



public final class Paths {
	private Paths() {}
	
	public static final String FAST_STATE_EXT = ".fast_state";
	public static final String FASTER_STATE_EXT = ".faster_state";
	public static final String LIBSVM_UNNORM_EXT = ".libsvm_unnorm";
	
	public static String outputDir() {
		return jhn.Paths.outputDir("EDA");
	}
		
		public static String defaultRunsDir() {
			return outputDir() + "/runs";
		}
		
			public static String runDir(String runsDir, int run) {
				return runsDir + "/" + String.valueOf(run);
			}
				
				public static String nextRunDir(String runsDir) {
					return runDir(runsDir, nextRun(runsDir));
				}
				
				public static String sampleSummaryFilename(String runDir, int lastN) {
					return runDir + "/aggregate_last" + lastN + jhn.Paths.STATE_EXT;
				}
				
				public static String sampleSummaryFilename(String summarizerName, String runDir, int start, int stop, int minCount) {
					return sampleSummaryFilename(summarizerName, runDir, start, stop, minCount, false);
				}
				
				public static String sampleSummaryKey(String summarizerName, int start, int stop, int minCount, boolean includesClass) {
					StringBuilder s = new StringBuilder();
					s.append(summarizerName);
					s.append("_iters");
					s.append(start);
					s.append('-');
					s.append(stop);
					s.append("_min");
					s.append(minCount);
					if(includesClass) {
						s.append("_classy");
					}
					return s.toString();
				}
				
				public static String sampleSummaryFilename(String summarizerName, String runDir, int start, int stop, int minCount, boolean includesClass) {
					StringBuilder s = new StringBuilder(runDir);
					s.append("/summary-");
					s.append(sampleSummaryKey(summarizerName, start, stop, minCount, includesClass));
					s.append(jhn.Paths.STATE_EXT);
					return s.toString();
				}
				
				public static String sampleSummaryFilename(String runDir, SummaryParams sp) {
					return sampleSummaryFilename(sp.summarizerName(), runDir, sp.firstIter, sp.lastIter, sp.minCount, sp.includeClass);
				}
				
				public static String stateDir(String runDir) {
					return runDir + "/state";
				}
					
						public static String stateFilename(String runDir, int iteration) {
							return stateDir(runDir) + "/" + iteration + jhn.Paths.STATE_EXT;
						}
				
				public static String fastStateDir(String runDir) {
					return runDir + "/fast_state";
				}
					
					public static String fastStateFilename(String runDir, int iteration) {
						return fastStateDir(runDir) + "/" + iteration + FAST_STATE_EXT;
					}
				
				public static String fasterStateDir(String runDir) {
					return runDir + "/faster_state";
				}
					public static String fasterStateFilename(String runDir, int iteration) {
						return fasterStateDir(runDir) + "/" + iteration + FASTER_STATE_EXT;
					}

				
				
				public static String reducedDir(String runDir) {
					return runDir + "/reduced";
				}
				
					public static String libSvmReducedFilename(String runDir, int iteration, boolean normalize) {
						return runDir + "/" + iteration + (normalize ? jhn.Paths.LIBSVM_EXT : LIBSVM_UNNORM_EXT);
					}
				
				
				public static String modelDir(String runDir) {
					return runDir + "/model";
				}
				
					public static String modelFilename(String runDir, int iteration) {
						return modelDir(runDir) + "/" + iteration + ".ser";
					}
				
				public static String docTopicsDir(String runDir) {
					return runDir + "/doctopics";
				}
				
					public static String docTopicsFilename(String runDir, int iteration) {
						return docTopicsDir(runDir) + "/" + iteration + jhn.Paths.DOCTOPICS_EXT;
					}
				
				public static String topDocTopicsDir(String runDir) {
					return runDir + "/top_doc_topics";
				}
				
					public static String topDocTopicsFilename(String runDir, int iteration) {
						return topDocTopicsDir(runDir) + "/" + iteration + ".log";
					}
				
				public static String topTopicWordsDir(String runDir) {
					return runDir + "/top_topic_words";
				}
				
					public static String topTopicWordsFilename(String runDir, int iteration) {
						return topTopicWordsDir(runDir) + "/" + iteration + ".log";
					}
				
				public static String topicLabelHitDataFilename(String runDir, int iteration) {
					return runDir + "/topic_label_hit_data_it" + iteration + ".csv";
				}
	
	public static int nextRun(String runsDir) {
		return jhn.Paths.nextRun(runsDir);
	}
}
