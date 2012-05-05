#!/bin/sh
../topicalguide/tools/mallet/mallet import-dir --input ./toy_dataset --output ./toy_dataset.mallet --remove-stopwords --keep-sequence --set-source-by-name --token-regex "[a-zA-Z]+" --source-name-prefix "file:/home/jjfresh/Projects/eda_java/toy_dataset/"

