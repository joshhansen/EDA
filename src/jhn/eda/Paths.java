package jhn.eda;


public final class Paths {
	private Paths() {}
	
	public static String outputDir() {
		return jhn.Paths.outputDir("EDA");
	}
	
		public static String propsDir() {
			return outputDir() + "/props";
		}
			public static String propsFilename(String topicWordIdxName, String datasetName, int minCount) {
				return propsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + ".conf.ser";
			}
	
		public static String countsDir() {
			return outputDir() + "/counts";
		}
		
			public static String topicCountsDir() {
				return countsDir() + "/topics";
			}
				public static String topicCountsFilename(String topicWordIdxName, String datasetName, int minCount) {
					return topicCountsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + ".ser";
				}
				
				/** Topic counts that are sums of type-topic counts where type is in target corpus and count >= minCount */
				public static String restrictedTopicCountsFilename(String topicWordIdxName, String datasetName, int minCount) {
					return topicCountsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + "_restricted.ser";
				}
				
				/** Topic counts that are sums of type-topic counts where topic has at least one type in corpus and count >= minCount */
				public static String filteredTopicCountsFilename(String topicWordIdxName, String datasetName, int minCount) {
					return topicCountsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + "_filtered.ser";
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
					return runDir + "/aggregate_last" + lastN + ".state";
				}
				
				public static String sampleSummaryFilename(String runDir, int lastN, int minCount) {
					return runDir + "/aggregate_last" + lastN + "_min" + minCount + ".state";
				}
				
				public static String stateDir(String runDir) {
					return runDir + "/state";
				}
					
						public static String stateFilename(String runDir, int iteration) {
							return stateDir(runDir) + "/" + iteration + ".state";
						}
				
				public static String fastStateDir(String runDir) {
					return runDir + "/fast_state";
				}
					
					public static String fastStateFilename(String runDir, int iteration) {
						return fastStateDir(runDir) + "/" + iteration + ".log";
					}
				
				
				public static String reducedDir(String runDir) {
					return runDir + "/reduced";
				}
				
					public static String libSvmReducedFilename(String runDir, int iteration, boolean normalize) {
						return runDir + "/" + iteration + (normalize ? ".libsvm" : ".libsvm_unnorm");
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
						return docTopicsDir(runDir) + "/" + iteration + ".log";
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
				
				public static String documentLabelHitDataFilename(String runDir, int iteration) {
					return runDir + "/document_label_hit_data_it" + iteration + ".csv";
				}
				
				public static String documentLabelHitDataFilename(String runDir, int lastN, int minCount) {
					return runDir + "/document_label_hit_data_last" + lastN + "_min" + minCount + ".csv";
				}
	
	public static String extractedDataID(String topicWordIdxName, String datasetName, int minCount) {
		return topicWordIdxName + ":" + datasetName + "_min" + minCount;
	}
	
	public static int nextRun(String runsDir) {
		return jhn.Paths.nextRun(runsDir);
	}

}
