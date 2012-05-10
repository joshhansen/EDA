package jhn.eda.lucene;

import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;

import jhn.wp.Fields;

class LuceneLabelAlphabet extends LabelAlphabet {
	private static final long serialVersionUID = 1L;
	private IndexSearcher s;
	public LuceneLabelAlphabet(IndexReader r) {
		this.s = new IndexSearcher(r);
	}
	
	@Override
	public int lookupIndex(Object entry, boolean addIfNotPresent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Label lookupLabel(Object entry, boolean addIfNotPresent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Label lookupLabel(Object entry) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Label lookupLabel(int labelIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int lookupIndex(Object entry) {
		Query q = new TermQuery(new Term(Fields.label, entry.toString()));
		try {
			TopDocs td = s.search(q, 1);
			return td.scoreDocs[0].doc;				
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public Object lookupObject(int index) {
		try {
			return s.doc(index).get(Fields.label);
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Object[] lookupObjects(int[] indices) {
		return lookupObjects(indices, new Object[indices.length]);
	}

	@Override
	public Object[] lookupObjects(int[] indices, Object[] buf) {
		for(int i = 0; i < indices.length; i++) {
			buf[i] = lookupObject(indices[i]);
		}
		return buf;
	}

	@Override
	public int[] lookupIndices(Object[] objects, boolean addIfNotPresent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return s.getIndexReader().numDocs();
	}
	
}