#!/bin/bash
javac -cp /home/jjfresh/Libraries/bzip2:../External/mallet-2.0.7/class:../External/mallet-2.0.7/lib/mallet-deps.jar:/home/jjfresh/Libraries/nxparser-1.2.1.jar:/home/jjfresh/Libraries/commons-lang3-3.1-src/bin:/home/jjfresh/Libraries/mongo-java-driver/bin:/home/jjfresh/Libraries/bliki_3.0.16/bliki-core/target/bliki-core-3.0.16.jar:/home/jjfresh/Libraries/wikixml-j/bin:$HOME/Libraries/lucene-3.5.0/lucene-core-3.5.0.jar -sourcepath ./src -d ./bin ./src/jhn/eda/LuceneTest.java