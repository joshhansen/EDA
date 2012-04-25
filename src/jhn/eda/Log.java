package jhn.eda;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Log {
	private PrintWriter log;
	public Log(String filename) {
		try {
			log = new PrintWriter(new FileWriter(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void flush() {
		System.out.flush();
		log.flush();
	}
	
	public void print(char c) {
		System.out.print(c);
		log.print(c);
		flush();
	}
	
	public void print(Object o) {
		System.out.print(o);
		log.print(o);
		flush();
	}
	public void println(Object o) {
		System.out.println(o);
		log.println(o);
	}
	public void println() {
		System.out.println();
		log.println();
	}
	
	public void println(int x) {
		System.out.println(x);
		log.println(x);
	}
	
	public void close() {
		log.close();
	}
}
