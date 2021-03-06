#!/bin/bash
# Generate the properties file and consequently execute the CollectionIndex program

cd ../

stopFilePath="/home/dwaipayan/smart-stopwords"
#toStore="NO" # YES/NO
toStore="YES" # YES/NO
storeTermVector="YES"
toRefine="false"
# bulkFileReadSize="10"
echo "Using stopFilePath="$stopFilePath
echo "Using toStore="$toStore
echo "Using storeTermVector="$storeTermVector
echo "Refining by dropping <html-tags> and urls="$toRefine


if [ $# -le 1 ] 
then
    echo "Usage: " $0 " <spec-path> <index-path> [dump-path]";
    echo "1. spec-path: ";
    echo "2. index-path: ";
    echo "3. [dump-path] - optional; specify if want to dump the index";
    exit 1;
fi


prop_name="build/classes/trec-index.properties"
spec_path=`readlink -f $1`		# absolute address of the .properties file


echo "Continue - Press any key? (Ctrl-C to quit)"
#sleep 3     # wait for 5 second before continuing

if [ ! -f $spec_path ]
then
    echo "Spec file not exists"
    exit 1;
fi
index_path=`readlink -f $2`		# absolute path of where to store the index

# making the .properties file in 'build/classes'
cat > $prop_name << EOL

collSpec=$spec_path

indexPath=$2

toStore=$toStore

storeTermVector=$storeTermVector

stopFilePath=$stopFilePath

toRefine=$toRefine

EOL

if [ $# -eq 3 ]
then
    cat >> $prop_name << EOL
dumpPath=$3
EOL
fi
# .properties file created in 'build/classes' 

echo $prop_name

java -cp dist/WebData.jar indexer.TrecDocIndexer $prop_name

cp $prop_name $index_path/.

echo "The .properties file is saved in the index directory: "$index_path/

