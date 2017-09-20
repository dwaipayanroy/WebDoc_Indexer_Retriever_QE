#!/bin/bash
# Generate the properties file and consequently execute the CollectionIndex program

cd ../

# readlink (for getting the absolute path) must be installed

stopFilePath="/home/dwaipayan/smart-stopwords"
toStore="NO" # YES/NO
#toStore="YES" # YES/NO

### NO / YES / WITH_POSITIONS / WITH_OFFSETS / WITH_POSITIONS_OFFSETS
#storeTermVector="NO"
storeTermVector="YES"

### Whether to remove URL and HTML-TAGS
#toRefine="false"
toRefine="true"

### For wt10g premble
#ignorePremble="false"
ignorePremble="true"

echo "Using stopFilePath= "$stopFilePath
echo "Using toStore= "$toStore
echo "Using storeTermVector= "$storeTermVector
echo "Refining by dropping <html-tags> and urls= "$toRefine
echo "Ignoring Premble= "$ignorePremble


if [ $# -le 3 ] 
then
    echo "Usage: " $0 " <collection-name> <spec-path> <index-path> <coll-type> [dump-path]";
    echo "1. collection-name: The prop file will be made having this name";
    echo "2. spec-path: ";
    echo "3. index-path: ";
    echo "4. collection-type: 1: Trec; 2: Web";
    echo "5. [dump-path] - optional; specify if want to dump the index";
    exit 1;
fi


prop_name=$1".index.properties"
spec_path=`readlink -f $2`		# absolute address of the .properties file


echo "Continue - Press any key? (Ctrl-C to quit)"
#sleep 3     # wait for 5 second before continuing

if [ ! -f $spec_path ]
then
    echo "Spec file not exists"
    exit 1;
fi
index_path=`readlink -f $3`		# absolute path of where to store the index

cd build/classes

# making the .properties file in 'build/classes'
cat > $prop_name << EOL

collSpec=$spec_path

indexPath=$3

toStore=$toStore

storeTermVector=$storeTermVector

collectionType=$4

stopFilePath=$stopFilePath

toRefine=$toRefine

ignorePremble=$ignorePremble

EOL

if [ $# -eq 5 ]
then
    cat >> $prop_name << EOL
dumpPath=$5
EOL
fi
# .properties file created in 'build/classes' 

java -cp $CLASSPATH:./lib/jsoup-1.7.3.jar:./lib/lucene-analyzers-common-4.10.4-SNAPSHOT.jar:./lib/lucene-core-4.10.4-SNAPSHOT.jar:./lib/lucene-queries-4.10.4-SNAPSHOT.jar:./lib/lucene-queryparser-4.10.4-SNAPSHOT.jar:./lib/sax2r2.jar trecdata.CollectionIndexer $prop_name

cp $prop_name $index_path/.

echo "The .properties file is saved in the index directory: "$index_path/

