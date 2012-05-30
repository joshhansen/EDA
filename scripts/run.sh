#!/bin/sh
. scripts/common.sh
MEM=3500m
java -Xmx$MEM -cp target/classes:$JHNC:$BZIP2:$MALLET:$MALLET_DEPS:$NXPARSER:$LANG3:$MONGO:$LUCENE:$FASTUTIL $1

