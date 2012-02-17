function m() {
    emit(this.w, this.c);
}

function r(key, listofcounts) {
	var totalAggregated = 0;
	var result = {};
	listofcounts.forEach(function(counts) {
		for(var count in counts) {
			var topics = counts[count];
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
db.word_label_counts_raw.mapReduce(m, r, {out:{replace:'word_label_counts'},jsMode:false,verbose:true});
