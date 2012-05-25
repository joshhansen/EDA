package jhn.eda;

public final class Paths {
	private Paths() {}
	
	public static String datasetsDir() {
		return System.getenv("HOME") + "/Projects/eda/datasets";
	}
	
		public static String datasetFilename(String datasetName) {
			return datasetsDir() + "/" + datasetName + ".mallet";
		}
	
	public static String outputDir() {
		return System.getenv("HOME") + "/Projects/eda_output";
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
				
				public static String restrictedTopicCountsFilename(String topicWordIdxName, String datasetName, int minCount) {
					return topicCountsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + "_restricted.ser";
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

		public static String labelAlphabetsDir() {
			return outputDir() + "/label_alphabets";
		}
			public static String labelAlphabetFilename(String topicWordIdxName, String datasetName, int minCount) {
				return labelAlphabetsDir() + "/" + extractedDataID(topicWordIdxName, datasetName, minCount) + ".ser";
			}
		
		public static String runsDir() {
			return outputDir() + "/runs";
		}
	
	public static String extractedDataID(String topicWordIdxName, String datasetName, int minCount) {
		return topicWordIdxName + ":" + datasetName + "_min" + minCount;
	}
}
