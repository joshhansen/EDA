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
		
		public static String defaultRunsDir() {
			return outputDir() + "/runs";
		}
		
			public static String runDir(String runsDir, int run) {
				return runsDir + "/" + String.valueOf(run);
			}
			
			@Deprecated
			public static String runDir(int run) {
				return runDir(defaultRunsDir(), run);
			}
				
				public static String nextRunDir(String runsDir) {
					return runDir(runsDir, nextRun(runsDir));
				}
				
				@Deprecated
				public static String nextRunDir() {
					return runDir(nextRun());
				}
				
				public static String sampleSummaryFilename(String runDir, int lastN) {
					return runDir + "/aggregate_last" + lastN + ".state";
				}
				
				@Deprecated
				public static String sampleSummaryFilename(int run, int lastN) {
					return sampleSummaryFilename(runDir(run), lastN);
				}
				
				public static String sampleSummaryFilename(String runDir, int lastN, int minCount) {
					return runDir + "/aggregate_last" + lastN + "_min" + minCount + ".state";
				}
				
				@Deprecated
				public static String sampleSummaryFilename(int run, int lastN, int minCount) {
					return sampleSummaryFilename(runDir(run), lastN, minCount);
				}
				
				public static String stateDir(String runDir) {
					return runDir + "/state";
				}
					@Deprecated
					public static String stateDir(int run) {
						return stateDir(runDir(run));
					}
					
						public static String stateFilename(String runDir, int iteration) {
							return stateDir(runDir) + "/" + iteration + ".state";
						}
						
						@Deprecated
						public static String stateFilename(int run, int iteration) {
							return stateFilename(runDir(run), iteration);
						}
				
				public static String fastStateDir(String runDir) {
					return runDir + "/fast_state";
				}
				
				@Deprecated
				public static String fastStateDir(int run) {
					return fastStateDir(runDir(run));
				}
					
					public static String fastStateFilename(String runDir, int iteration) {
						return fastStateDir(runDir) + "/" + iteration + ".log";
					}
					@Deprecated
					public static String fastStateFilename(int run, int iteration) {
						return fastStateFilename(runDir(run), iteration);
					}
				
				
				public static String reducedDir(String runDir) {
					return runDir + "/reduced";
				}
				
				@Deprecated
				public static String reducedDir(int run) {
					return reducedDir(runDir(run));
				}
				
					public static String libSvmReducedFilename(String runDir, int iteration, boolean normalize) {
						return runDir + "/" + iteration + (normalize ? ".libsvm" : ".libsvm_unnorm");
					}
					
					@Deprecated
					public static String libSvmReducedFilename(int run, int iteration, boolean normalize) {
						return libSvmReducedFilename(runDir(run), iteration, normalize);
					}
				
				
				public static String modelDir(String runDir) {
					return runDir + "/model";
				}
				
				@Deprecated
				public static String modelDir(int run) {
					return modelDir(runDir(run));
				}
				
					public static String modelFilename(String runDir, int iteration) {
						return modelDir(runDir) + "/" + iteration + ".ser";
					}
					
					@Deprecated
					public static String modelFilename(int run, int iteration) {
						return modelFilename(runDir(run), iteration);
					}
				
				public static String docTopicsDir(String runDir) {
					return runDir + "/doctopics";
				}
				
				@Deprecated
				public static String docTopicsDir(int run) {
					return docTopicsDir(runDir(run));
				}
				
					public static String docTopicsFilename(String runDir, int iteration) {
						return docTopicsDir(runDir) + "/" + iteration + ".log";
					}
					
					@Deprecated
					public static String docTopicsFilename(int run, int iteration) {
						return docTopicsFilename(runDir(run), iteration);
					}
				
				public static String topDocTopicsDir(String runDir) {
					return runDir + "/top_doc_topics";
				}
				
				@Deprecated
				public static String topDocTopicsDir(int run) {
					return topDocTopicsDir(runDir(run));
				}
				
					public static String topDocTopicsFilename(String runDir, int iteration) {
						return topDocTopicsDir(runDir) + "/" + iteration + ".log";
					}
					
					@Deprecated
					public static String topDocTopicsFilename(int run, int iteration) {
						return topDocTopicsFilename(runDir(run), iteration);
					}
				
				public static String topTopicWordsDir(String runDir) {
					return runDir + "/top_topic_words";
				}
				
				@Deprecated
				public static String topTopicWordsDir(int run) {
					return topTopicWordsDir(runDir(run));
				}
				
					public static String topTopicWordsFilename(String runDir, int iteration) {
						return topTopicWordsDir(runDir) + "/" + iteration + ".log";
					}
					
					@Deprecated
					public static String topTopicWordsFilename(int run, int iteration) {
						return topTopicWordsFilename(runDir(run), iteration);
					}
				
				public static String topicLabelHitDataFilename(String runDir, int iteration) {
					return runDir + "/topic_label_hit_data_it" + iteration + ".csv";
				}
				
				@Deprecated
				public static String topicLabelHitDataFilename(int run, int iteration) {
					return topicLabelHitDataFilename(runDir(run), iteration);
				}
				
				public static String documentLabelHitDataFilename(String runDir, int iteration) {
					return runDir + "/document_label_hit_data_it" + iteration + ".csv";
				}
				
				@Deprecated
				public static String documentLabelHitDataFilename(int run, int iteration) {
					return documentLabelHitDataFilename(runDir(run), iteration);
				}
				
				public static String documentLabelHitDataFilename(String runDir, int lastN, int minCount) {
					return runDir + "/document_label_hit_data_last" + lastN + "_min" + minCount + ".csv";
				}
				
				@Deprecated
				public static String documentLabelHitDataFilename(int run, int lastN, int minCount) {
					return documentLabelHitDataFilename(runDir(run), lastN, minCount);
				}
	
	public static String extractedDataID(String topicWordIdxName, String datasetName, int minCount) {
		return topicWordIdxName + ":" + datasetName + "_min" + minCount;
	}
	
	public static int nextRun(String runsDir) {
		return jhn.Paths.nextRun(runsDir);
	}
	
	@Deprecated
	public static int nextRun() {
		return nextRun(defaultRunsDir());
	}

}
