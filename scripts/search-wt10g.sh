#!/bin/bash
# WT10G searcher. Generate the properties file and consequently execute the CollectionSearcher

cd ../

echo "Set the index path of wt10g having two fields for text (1. refined, 2. full)"
indexPath="/store/collections/indexed/wt10g-2field.index/"
echo "Index Path: "$indexPath

#echo "Set query path of wt10g: "
queryPath="/home/dwaipayan/Dropbox/ir/corpora-stats/topics_xml/trec9.xml"
echo "Query Path: "$queryPath

if [ $# -le 1 ] 
then
    echo "Usage: " $0 " list-of-arguments";
    echo "1. field of the index to search: 1.Full, 2.Clean";
    echo "2. similarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity";
    echo "3. [param1]: 'k1'-BM25; lambda-LMJM; mu-LMDi";
    echo "4. [param2]: optional 'b' if using BM25";
    exit 1;
fi

# only the title field of topic will be used for searching
qff=1

if [ $1 = "1" ]
then
	fieldToSearch="full-content"
else
	fieldToSearch="content"
fi

sf=$2

prop_name="wt10g-searcher.properties"
#echo $prop_name

#cd build/classes

# making the .properties file
cat > $prop_name << EOL

stopFilePath=/home/dwaipayan/smart-stopwords

indexPath=$indexPath

fieldToSearch=$fieldToSearch

queryPath=$queryPath

## queryFields
# 1: title
# 2: title + desc
# 3. title + desc + narr
queryFieldFlag=$qff

### Similarity functions:
#0 - DefaultSimilarity
#1 - BM25Similarity
#2 - LMJelinekMercerSimilarity
#3 - LMDirichletSimilarity
similarityFunction=$sf

numHits=1000

EOL
if [ $# -ge 3 ]
then
    cat >> $prop_name << EOL
param1=$3
EOL
fi


if [ $# -eq 4 ]
then
    cat >> $prop_name << EOL
param2=$4
EOL
fi

java -cp dist/WebData.jar searcher.WebDocSearcher $prop_name

