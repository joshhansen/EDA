#!/bin/bash
source scripts/common.sh
javac -cp $BZIP2:$MALLET:$MALLET_DEPS:$NXPARSER:$LANG3:$MONGO:$BLIKI:$WIKIXML_J:$LUCENE:$FASTUTIL:$JHNC -sourcepath ./src -d ./bin ./src/jhn/eda/lucene/LuceneEDA.java

