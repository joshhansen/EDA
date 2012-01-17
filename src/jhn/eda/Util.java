package jhn.eda;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.tools.bzip2.CBZip2InputStream;

public final class Util {
	private Util(){}
	
	public static InputStream smartInputStream(final String filename) throws IOException {
		if(filename.endsWith(".bz2")) {
			InputStream is = new FileInputStream(filename);
			is.read();
			is.read();
			return new CBZip2InputStream(is);
		} else {
			return new FileInputStream(filename);
		}
	}
	
	public static Reader smartReader(final String filename) throws IOException {
		return new InputStreamReader(smartInputStream(filename));
	}
}
