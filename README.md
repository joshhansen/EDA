# Explicit Dirichlet Allocation

Explicit Dirichlet Allocation (EDA) is a probabilistic topic model utilizing explicit (predefined) rather than latent (discovered) topics.

This codebase actually implements two related algorithms. The first, LDA with Static Topic-Word Distributions (LDA-STWD) is implemented by jhn.eda.LDASTWD. The second, Explicit Dirichlet Allocation (EDA) is implemented by jhn.eda.EDA. Both inherit some common functionality from jhn.eda.ProbabilisticExplicitTopicModel.

## Usage
Prerequisites:
* First, you need a Mallet imported dataset (InstanceList). See http://mallet.cs.umass.edu/import.php for more information on this.
* Second, grab a copy of Wikipedia's article text and generate a topic-word index. Wikipedia article text can be downloaded at https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2. The topic-word index is basically a fulltext index of the Wikipedia articles, such that each article becomes one document in the index. Lucene is used for indexing. See jhn.wp.fulltext.IndexFullText in the JhnCommon repository for an example of how this is done.
* Third, convert the topic-word index into arrays. The conversion is done using jhn.eda.CountsExtractor to convert this index into an array representation---this allows the data to be kept in RAM, thus improving performance (if you have enough RAM for the data, that is.)
* Finally, assemble all the extracted data together and run jhn.eda.RunProbabilisticExplicitTopicModel. This class runs any descendant of ProbabilisticExplicitTopicModel on the extracted data and records the results. Which results are recorded is controlled by RunProbabilisticExplicitTopicModel.addListeners(ProbabilisticExplicitTopicModel), which adds listeners to the model that accomplish various reporting tasks.
