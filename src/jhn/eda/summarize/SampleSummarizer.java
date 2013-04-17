package jhn.eda.summarize;

import java.io.File;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import jhn.counts.i.i.i.IntIntIntCounterMap;

public interface SampleSummarizer {
	IntIntIntCounterMap summarize(File[] fastStateFiles, Int2ObjectMap<String> sources) throws Exception;
}
