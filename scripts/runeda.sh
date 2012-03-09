#!/bin/bash
MEM=2500m
LIB=$HOME/Libraries
PRJ=$HOME/Projects
EXT=$PRJ/External
java -Xmx$MEM -cp bin:$PRJ/JhnCommon/bin:$LIB/bzip2:$EXT/mallet-2.0.7/class:$EXT/mallet-2.0.7/lib/mallet-deps.jar:$LIB/nxparser-1.2.1.jar:$LIB/commons-lang3-3.1-src/bin:$LIB/mongo-java-driver/bin:$LIB/lucene-3.5.0/lucene-core-3.5.0.jar jhn.eda.lucene.LuceneEDA
