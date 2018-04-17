#!/bin/bash
# Prints all the document-length in both 1. full and 2. clean field of the index.

cd ../

if [ $# -lt 1 ] 
then
    echo "Usage: " $0 " index-path [-d (to compute all doc.length)] [-t <field-name> (to get the unique term count in field-name)]";
    exit 1;
fi

indexPath=$1
if [ $2 == "-d" ]
then
    java -cp $CLASSPATH:dist/WebData.jar common.CollectionStatistics $indexPath -d
else
    if [ $# -lt 3 ] 
    then 
        echo "Usage: " $0 " index-path [-d (to compute all doc.length)] [-t <field-name> (to get the unique term count in field-name)]";
        exit 1;
    else
        java -cp $CLASSPATH:dist/WebData.jar common.CollectionStatistics $indexPath -t $3
    fi
fi


