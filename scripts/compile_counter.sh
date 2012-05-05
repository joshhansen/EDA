#!/bin/sh
. scripts/common.sh
javac -cp $BZIP2:$MALLET:$MALLET_DEPS:$NXPARSER:$LANG3:$MONGO:$BLIKI:$WIKIXML_J:$LUCENE:$JHNC -sourcepath ./src -d ./bin ./src/jhn/wp/articles/IndexArticles.java

