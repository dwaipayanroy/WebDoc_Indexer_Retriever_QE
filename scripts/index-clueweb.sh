#!/bin/bash
# readlink (for getting the absolute path) must be installed

# Generates the properties file and consequently execute the ClueWebIndexer program

# Generates three field Lucene index: 1. docid, 2. full-content, 3. content.
# 1. docid: unique document-id of the document
# 2. full-content: entire content of each document, including all html-tags, urls, meta-info., excluding warc structural info.
# 3. content: clean content of each document, excluding all tags, urls, meta-info. etc.

# If you want to store the content that gets filtered during the cleaning,
#   uncomment the last portion of processDocument() in src/indexer/DocumentProcessing.java

cd ../

homepath=`eval echo ~$USER`
stopFilePath="$homepath/smart-stopword"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
else
    echo "Using stopFilePath="$stopFilePath
fi

toStore="NO" # YES/NO
#toStore="YES" # YES/NO
storeTermVector="YES"
echo "Storing the content in index: "$toStore
echo "Storing the term-vectors: "$storeTermVector


if [ $# -le 1 ] 
then
    echo "Usage: " $0 " <spec-path> <index-path> [spam-score-index-path]";
    echo "1. spec-path: Path of the spec file containing the warc.gz files, one in each line";
    echo "2. index-path: Path, where the index will be created";
    echo "3. [spam-score-index-path] - optional; index-path of the Waterloo spam score";
    exit 1;
fi


prop_name="build/classes/clueweb-indexer"$#".properties"
spec_path=`readlink -f $1`		# absolute address of the .spec file


if [ ! -f $spec_path ]
then
    echo "Spec file not exists"
    exit 1;
fi
index_path=`readlink -f $2`		# absolute path of where to store the index

# making the .properties file in 'build/classes'
cat > $prop_name << EOL

collSpec=$spec_path

indexPath=$index_path

toStore=$toStore

storeTermVector=$storeTermVector

stopFilePath=$stopFilePath

EOL

if [ $# -eq 3 ]
then
    cat >> $prop_name << EOL
spamScoreIndexPath=$3

spamScoreThreshold=69

EOL
fi

# .properties file created in 'build/classes' 

java -cp $CLASSPATH:dist/WebData.jar:./lib/* indexer.ClueWebIndexer $prop_name

cp $prop_name $index_path/.

echo "The .properties file is saved in the index directory: "$index_path/

