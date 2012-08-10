package jhn.eda;

public enum Options {
	MIN_THREADS,
	MAX_THREADS,
	
	// Feature Selection, etc.
	FILTER_DIGITS,
	
	FILTER_MONTHS,
	
//	TFIDF_TOP10,
	PRESELECTED_FEATURES,
	
	
	// EDA Model Parameters
	/** Dirichlet(alpha,alpha,...) is the distribution over topics */
	ALPHA,
	
	/** Prior on per-topic multinomial distribution over words */
	BETA,
	
	ALPHA_SUM,
	
	BETA_SUM,
	
	ALPHA_OPTIMIZE_INTERVAL,
	
	/** The number of topics requested */
	NUM_TOPICS,
	
	/** The size of the vocabulary */
	NUM_TYPES,
	
	ITERATIONS,
	
}
