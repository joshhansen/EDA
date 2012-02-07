function m() {
    emit(this.w, this.c);
}

function r(key, listofcounts) {
	var totalAggregated = 0;
	var result = {};
	listofcounts.forEach(function(counts) {
		for(var count in counts) {
			var topics = counts[count];
			if(!(topics instanceof Array)) {
				topics = [topics];
			}
			if(count in result) {
				result[count] = result[count].concat(topics);
			} else {
				result[count] = topics;
			}
			totalAggregated += topics.length;
		}
	});
	print(totalAggregated + "\t-> " + key);
	return result;
}
db.topic_word_counts_unreduced.mapReduce(m, r, {out:{replace:'topic_word_counts_redux'},jsMode:false,verbose:true});