#!/bin/bash
source scripts/common.sh

# Intense GC options: -Xms1024m -Xmx2500m -XX:NewSize=256m -XX:MaxNewSize=256m -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:PermSize=128m -XX:MaxPermSize=128m
MEM=2500m

java -Xmx$MEM -cp bin:$BZIP2:$MALLET:$MALLET_DEPS:$NXPARSER:$LANG3:$MONGO:$BLIKI:$WIKIXML_J:$LUCENE:$JHNC jhn.wp.articles.IndexArticles

