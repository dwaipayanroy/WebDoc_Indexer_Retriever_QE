#!/bin/bash
# Generate the properties file and consequently execute the rblm program

cd ../

homepath=`eval echo ~$USER`
stopFilePath="$homepath/smart-stopwords"

indexPath="/store/collections/indexed/wt10g-2field.index/"
queryPath="/home/dwaipayan/Dropbox/ir/corpora-stats/topics_xml/wt10g.xml"
qrelPath="/home/dwaipayan/Dropbox/ir/corpora-stats/qrels/all.qrel"
resPath="$homepath/"
# feedbackFilePath=""

echo "Using stopFilePath="$stopFilePath
echo "Using indexPath="$indexPath
echo "Using queryPath="$queryPath
echo "Using resPath="$resPath

toTRF=false

if [ $# -le 5 ] 
then
    echo "Usage: " $0 " <no.-of-pseudo-rel-docs> <no.-of-expansion-terms> <query-mix (default-0.98)>";
    echo "1. Number of expansion documents";
    echo "2. Number of expansion terms";
    echo "3. RM3 - QueryMix:";
    echo "4. Field to be used for Search. 1.Full, 2.Clean";
    echo "5. Field to be used for Feedback. 1.Full, 2.Clean";
    echo "6. Similarity Function: 1:BM25, 2: LM-JM, 3: LM-Dir";
    echo "7. [Rerank]? - Yes-1  No-0 (default)"
    exit 1;
fi

prop_name="rblm.D-"$1".T-"$2".S-"$4".F-"$5".properties"
#echo $prop_name

if [ $4 == "1" ]
then
    fieldToSearch="full-content"
else
    fieldToSearch="content"
fi

if [ $5 == "1" ]
then
    fieldForFeedback="full-content"
else
    fieldForFeedback="content"
fi

echo "Field for searching="$fieldToSearch
echo "Field for feedback="$fieldForFeedback

similarityFunction=$6

case $similarityFunction in
    1) param1=1.2
       param2=0.75 ;;
    2) param1=0.5
       param2=0.0 ;;
    3) param1=2000
       param2=0.0 ;;
    4) param1=0.0    # dummy parameters for DFR; Config. choose is IFB2
       param2=0.0 ;; # dummy
esac

echo "similarity-function: "$similarityFunction" " $param1 ", "$param2


if [ $# -eq 7 ] && [ $7 = "1" ]
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

fieldForFeedback=$fieldForFeedback

queryPath=$queryPath

stopFilePath=$stopFilePath

resPath=$resPath

numHits= 1000

similarityFunction=$similarityFunction

param1=$param1
param2=$param2

# Number of documents
numFeedbackDocs=$1

# Number of terms
numFeedbackTerms=$2

rm3.queryMix=$3

rm3.rerank=$rerank

qrelPath=$qrelPath

toTRF=$toTRF

feedbackFromFile=false

feedbackFilePath=$feedbackFilePath

EOL
# .properties file made

java -Xmx1g -cp $CLASSPATH:dist/WebData.jar RelevanceFeedback.RelevanceBasedLanguageModel $prop_name

