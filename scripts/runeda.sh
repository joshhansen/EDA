#!/bin/bash
LIB=$HOME/Libraries
EXT=$HOME/Projects/External
java -Xmx10g -cp bin:$LIB/bzip2:$EXT/mallet-2.0.7/class:$EXT/mallet-2.0.7/lib/mallet-deps.jar:$LIB/nxparser-1.2.1.jar:$LIB/commons-lang3-3.1-src/bin:$LIB/mongo-java-driver/bin:$LIB/bliki_3.0.16/bliki-core/target/bliki-core-3.0.16.jar:$LIB/wikixml-j/bin:$LIB/lucene-3.5.0/lucene-core-3.5.0.jar jhn.eda.lucene.LuceneEDA
