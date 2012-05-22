#!/bin/sh
. scripts/common.sh
MEM=3000m
java -Xmx$MEM -cp bin:$JHNC:$BZIP2:$MALLET:$MALLET_DEPS:$NXPARSER:$LANG3:$MONGO:$LUCENE:$FASTUTIL $1

