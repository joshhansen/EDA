#!/bin/sh
. scripts/common.sh
javac -cp $BZIP2:$MALLET:$MALLET_DEPS:$NXPARSER:$LANG3:$MONGO:$BLIKI:$WIKIXML_J:$LUCENE:$JHNC:$FASTUTIL -sourcepath ./src -d ./bin $1

