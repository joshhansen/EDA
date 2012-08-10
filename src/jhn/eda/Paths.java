package jhn.eda;


public final class Paths {
	private Paths() {}
	
	public static String datasetsDir() {
		return jhn.Paths.outputDir("LDA") + "/datasets";
	}
	
		public static String datasetFilename(String datasetName) {
			return datasetsDir() + "/" + datasetName + ".mallet";
		}
	
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
		
		public static String indicesDir() {
			return outputDir() + "/indices";
		}
		
			public static String indexDir(String indexName) {
				return indicesDir() + "/" + indexName;
			}
			
				public static String topicWordIndicesDir() {
					return indexDir("topic_words");
				}
			
				public static String topicWordIndexDir(String name) {
					return topicWordIndicesDir() + "/" + name;
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
		
		public static String runsDir() {
			return outputDir() + "/runs";
		}
			public static String runDir(int run) {
				return runsDir() + "/" + String.valueOf(run);
			}
				public static String nextRunDir() {
					return runDir(nextRun());
				}
				
				public static String sampleSummaryFilename(int run, int lastN) {
					return runDir(run) + "/aggregate_last" + lastN + ".state";
				}
				
				public static String sampleSummaryFilename(int run, int lastN, int minCount) {
					return runDir(run) + "/aggregate_last" + lastN + "_min" + minCount + ".state";
				}
				
				public static String stateDir(int run) {
					return runDir(run) + "/state";
				}
				
					public static String stateFilename(int run, int iteration) {
						return stateDir(run) + "/" + iteration + ".state";
					}
				
				public static String fastStateDir(int run) {
					return runDir(run) + "/fast_state";
				}
					public static String fastStateFilename(int run, int iteration) {
						return fastStateDir(run) + "/" + iteration + ".fast_state";
					}
				
				public static String reducedDir(int run) {
					return runDir(run) + "/reduced";
				}
				
					public static String libSvmReducedFilename(int run, int iteration, boolean normalize) {
						return runDir(run) + "/" + iteration + (normalize ? ".libsvm" : ".libsvm_unnorm");
					}
				
				public static String modelDir(int run) {
					return runDir(run) + "/model";
				}
				
					public static String modelFilename(int run, int iteration) {
						return modelDir(run) + "/" + iteration + ".ser";
					}
				
				public static String docTopicsDir(int run) {
					return runDir(run) + "/doctopics";
				}
				
					public static String docTopicsFilename(int run, int iteration) {
						return docTopicsDir(run) + "/" + iteration + ".log";
					}
				
				public static String topDocTopicsDir(int run) {
					return runDir(run) + "/top_doc_topics";
				}
					public static String topDocTopicsFilename(int run, int iteration) {
						return topDocTopicsDir(run) + "/" + iteration + ".log";
					}
					
				public static String topTopicWordsDir(int run) {
					return runDir(run) + "/top_topic_words";
				}
					public static String topTopicWordsFilename(int run, int iteration) {
						return topTopicWordsDir(run) + "/" + iteration + ".log";
					}
				
				public static String topicLabelHitDataFilename(int run, int iteration) {
					return runDir(run) + "/topic_label_hit_data_it" + iteration + ".csv";
				}
				
				public static String documentLabelHitDataFilename(int run, int iteration) {
					return runDir(run) + "/document_label_hit_data_it" + iteration + ".csv";
				}
				
				public static String documentLabelHitDataFilename(int run, int lastN, int minCount) {
					return runDir(run) + "/document_label_hit_data_last" + lastN + "_min" + minCount + ".csv";
				}
	
	public static String extractedDataID(String topicWordIdxName, String datasetName, int minCount) {
		return topicWordIdxName + ":" + datasetName + "_min" + minCount;
	}
	
	public static int nextRun() {
		return jhn.Paths.nextRun(runsDir());
	}

}
