#!/bin/bash

cd ../

# readlink (for getting the absolute path) must be installed

if [ $# -le 1 ] 
then
    echo "Usage: " $0 " <-index INDEX_PATH> <-spam SPAM-SCORE-FILE-PATH> <-percentile SPAM-SCORE>";
    echo "1. INDEX-PATH: Path of the directory where the spam-index will be stored.";
    echo "2. SPAM-SCORE-FILE-PATH: Path, where the two column file with (docid, spam-score) pair is stored.";
    echo "3. SPAM-SCORE: The percentage score below which, document will be considered as spam.";
    exit 1;
fi

index_path=`readlink -f $1`	# absolute path of where to store the index
spamfile_path=`readlink -f $2`  # absolute path of the Waterloo spam-score file
percentile=$3

if [ ! -f $spamfile_path ]
then
    echo "Spam file not exists in "$spamfile_path
    exit 1;
fi

java -cp $CLASSPATH:dist/WebData.jar:./lib/* indexer.ClueWebSpamFiltering -index $index_path -spam $spamfile_path -percentile $percentile

