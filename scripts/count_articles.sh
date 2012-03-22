#!/bin/bash
# Intense GC options: -Xms1024m -Xmx2500m -XX:NewSize=256m -XX:MaxNewSize=256m -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:PermSize=128m -XX:MaxPermSize=128m
MEM=2500m
LIB=$HOME/Libraries
EXTPRJ=$HOME/Projects/External
java -Xmx$MEM -cp bin:$LIB/bzip2:$EXTPROJ/mallet-2.0.7/class:$EXTPROJ/mallet-2.0.7/lib/mallet-deps.jar:$LIB/nxparser-1.2.1.jar:$LIB/commons-lang3-3.1-src/bin:$LIB/mongo-java-driver/bin:$LIB/bliki_3.0.16/bliki-core/target/bliki-core-3.0.16.jar:$LIB/wikixml-j/bin:$LIB/lucene-3.5.0/lucene-core-3.5.0.jar:$HOME/Projects/JhnCommon/bin jhn.wp.articles.CountArticles
