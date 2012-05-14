package jhn.eda;

public final class Options {
	private Options() {}
	
	// Feature Selection
	public static final String TYPE_TOPIC_MIN_COUNT = "type_topic_min_count";
	
	public static final String FILTER_DIGITS = "filter_digits";
	
	public static final String FILTER_MONTHS = "filter_months";
	
//	public static final String TFIDF_TOP10 = "tfidf_top10";
	public static final String PRESELECTED_FEATURES = "preselected_features";
	
	
	// EDA Model Parameters
	/** Dirichlet(alpha,alpha,...) is the distribution over topics */
	public static final String ALPHA = "alpha";
	
	/** Prior on per-topic multinomial distribution over words */
	public static final String BETA = "beta";
	
	public static final String ALPHA_SUM = "alpha_sum";
	
	public static final String BETA_SUM = "beta_sum";
	
	/** The number of topics requested */
	public static final String NUM_TOPICS = "num_topics";
	
	/** The size of the vocabulary */
	public static final String NUM_TYPES = "num_types";
	
	// Console and Log Output
	public static final String SHOW_TOPICS_INTERVAL = "show_topics_interval";
	
	public static final String PRINT_LOG_LIKELIHOOD = "print_log_likelihood";
}
