function m() {
	print("emit("+this.w+","+tojson(this.c)+")");
    emit(this.w, this.c);
}

function r(key, listofcounts) {
	print("r("+key+","+tojson(listofcounts)+")");
	var result = {};
	listofcounts.forEach(function(counts)) {
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
		}
	});
	print("\tresult: "+tojson(result));
	return result;
}