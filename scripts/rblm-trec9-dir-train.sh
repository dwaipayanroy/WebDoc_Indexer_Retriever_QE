#!/bin/bash
# Generate the properties file and consequently execute the rblm program

cd ../

stopFilePath="/home/dwaipayan/smart-stopwords"
indexPath="/store/collections/indexed/wt10g-2field.index/"
queryPath="/home/dwaipayan/Dropbox/ir/corpora-stats/topics_xml/trec9.xml"
resPath="/home/dwaipayan/train-trec9-rm3/dir/"
feedbackFilePath="/home/dwaipayan/Dropbox/paper_submitted/neuir2016-wordvecsim/final-draft/results/res-k-100-centroid-0.4-docOne/trec678-robust.centroid-link-100-0.4-weightedFalse-docformOne.res"

fieldToSearch="full-content"
fieldForRF="content"

echo "Using stopFilePath="$stopFilePath
echo "Using indexPath="$indexPath
echo "Using queryPath="$queryPath
echo "Using resPath="$resPath

echo "Field for searching="$fieldToSearch
echo "Field for feedback="$fieldForRF

if [ $# -le 2 ] 
then
    echo "Usage: " $0 " <no.-of-pseudo-rel-docs> <no.-of-expansion-terms> <query-mix (default-0.98)>";
    echo "1. Number of expansion documents";
    echo "2. Number of expansion terms";
    echo "3. RM3 - QueryMix:";
    echo "4. [Rerank]? - Yes-1  No-0 (default)"
    exit 1;
fi

prop_name="rblm-dir-D-"$1".T-"$2".properties"


if [ $# -eq 4 ] && [ $4 = "1" ]
then
    rerank="true"
    echo "Reranking"
else
    rerank="false"
    echo "Re-retrieving"
fi

# making the .properties file
cat > $prop_name << EOL

indexPath=$indexPath

fieldToSearch=$fieldToSearch

fieldForFeedback=$fieldForRF

queryPath=$queryPath

stopFilePath=$stopFilePath

resPath=$resPath

numHits= 1000

### Similarity functions:
#0 - DefaultSimilarity
#1 - BM25Similarity
#2 - LMJelinekMercerSimilarity
#3 - LMDirichletSimilarity
similarityFunction=3

param1=2000
param2=0.0

# Number of documents
numFeedbackDocs=$1

# Number of terms
numFeedbackTerms=$2

rm3.queryMix=$3

rm3.rerank=$rerank

feedbackFromFile=false

feedbackFilePath=$feedbackFilePath

EOL
# .properties file made

java -Xmx1g -cp dist/WebData.jar RelevanceFeedback.RelevanceBasedLanguageModel $prop_name

