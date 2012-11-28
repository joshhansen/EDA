package jhn.eda;



public final class Paths {
	private Paths() {}
	
	public static final String FAST_STATE_EXT = ".fast_state";
	public static final String FASTER_STATE_EXT = ".faster_state";
	public static final String PROPS_EXT = ".conf.ser";
	public static final String TOPIC_COUNTS_EXT = ".topic_counts";
	public static final String LIBSVM_EXT = ".libsvm";
	public static final String LIBSVM_UNNORM_EXT = ".libsvm_unnorm";
	
	public static String outputDir() {
		return jhn.Paths.outputDir("EDA");
	}
	
		public static String propsDir() {
			return outputDir() + "/props";
		}
			public static String propsFilename(String topicWordIdxName, String datasetName, int minCount) {
				return propsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + PROPS_EXT;
			}
	
		public static String countsDir() {
			return outputDir() + "/counts";
		}
		
			public static String topicCountsDir() {
				return countsDir() + "/topics";
			}
				public static String topicCountsFilename(String topicWordIdxName, String datasetName, int minCount) {
					return topicCountsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + TOPIC_COUNTS_EXT;
				}
				
				/** Topic counts that are sums of type-topic counts where type is in target corpus and count >= minCount */
				public static String restrictedTopicCountsFilename(String topicWordIdxName, String datasetName, int minCount) {
					return topicCountsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + "_restricted" + TOPIC_COUNTS_EXT;
				}
				
				/** Topic counts that are sums of type-topic counts where topic has at least one type in corpus and count >= minCount */
				public static String filteredTopicCountsFilename(String topicWordIdxName, String datasetName, int minCount) {
					return topicCountsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + "_filtered" + TOPIC_COUNTS_EXT;
				}
			
			public static String typeTopicCountsDir() {
				return countsDir() + "/type_topics";
			}
				public static String typeTopicCountsFilename(String topicWordIdxName, String datasetName, int minCount) {
					return typeTopicCountsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + ".ser";
				}
			
			public static String topicMappingsDir() {
				return outputDir() + "/topic_mappings";
			}
				public static String topicMappingFilename(String topicWordIdxName, String datasetName, int minCount) {
					return topicMappingsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + ".ser";
				}

		public static String labelAlphabetsDir() {
			return outputDir() + "/label_alphabets";
		}
			public static String labelAlphabetFilename(String topicWordIdxName, String datasetName, int minCount) {
				return labelAlphabetsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + ".ser";
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
						return runDir + "/" + iteration + (normalize ? LIBSVM_EXT : LIBSVM_UNNORM_EXT);
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
	
	public static String extractedDataID(String topicWordIdxName, String datasetName, int minCount) {
		return topicWordIdxName + ":" + datasetName + "_min" + minCount;
	}
	
	public static int nextRun(String runsDir) {
		return jhn.Paths.nextRun(runsDir);
	}
}
