#!/bin/bash
# Generate the properties file and consequently execute the CollectionIndex program

cd ../

# readlink (for getting the absolute path) must be installed

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
    echo "Usage: " $0 " <spec-path> <index-path> <coll-type> [dump-path]";
    echo "1. spec-path: ";
    echo "2. index-path: ";
    echo "3. [dump-path] - optional; specify if want to dump the index";
    exit 1;
fi

prop_name="build/classes/webdoc-indexer.properties"
spec_path=`readlink -f $1`		# absolute address of the .properties file

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
dumpPath=$3
EOL
fi
# .properties file created in 'build/classes' 

java -cp $CLASSPATH:dist/WebData.jar:./lib/* indexer.WebDocIndexer $prop_name

cp $prop_name $index_path/.

echo "The .properties file is saved in the index directory: "$index_path/

