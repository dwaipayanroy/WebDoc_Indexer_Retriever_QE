#!/bin/bash
# Prints all the document-length in both 1. full and 2. clean field of the index.

cd ../

echo "Set the index path of having two fields for text (1. refined, 2. full)"

if [ $# -lt 1 ] 
then
    echo "Usage: " $0 " index-path";
    exit 1;
fi

indexPath=$1

java -cp $CLASSPATH:dist/WebData.jar common.CollectionStatistics $indexPath

