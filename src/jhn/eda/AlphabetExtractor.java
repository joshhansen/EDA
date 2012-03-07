package jhn.eda;

import java.io.File;

import jhn.util.Util;

import cc.mallet.types.InstanceList;

public class AlphabetExtractor {
	public static void main(String[] args) {
		final String destDir = System.getenv("HOME") + "/Projects/eda_output";
		final String datasetFilename = System.getenv("HOME") + "/Projects/topicalguide/datasets/state_of_the_union/imported_data.mallet";
		
		InstanceList training = InstanceList.load(new File(datasetFilename));
		Util.serialize(training.getAlphabet(), destDir + "/state_of_the_union-alphabet.ser");
	}
}
