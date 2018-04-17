#!/bin/bash
# Generate the properties file and consequently execute the CollectionSearcher

cd ../

homepath=`eval echo ~$USER`
stopFilePath="$homepath/smart-stopwords"
if [ ! -f $stopFilePath ]
then
    echo "Please ensure that the path of the stopword-list-file is set in the .sh file."
else
    echo "Using stopFilePath="$stopFilePath
fi
resPath=$homepath

echo $#
if [ $# -le 4 ] 
then
    echo "Usage: " $0 " list-of-arguments";
    echo "1. indexPath: Path of the index";
    echo "2. fieldToSearch: Field name of the index to be used for searching (full-content / content)";
    echo "3. queryPath: path of the query file (in proper xml format)"
    echo "4. queryFieldFlag: 1-title, 2-title+desc, 3-title+desc+narr";
    echo "5. similarityFunction: 0.DefaultSimilarity, 1.BM25, 2.LMJelinekMercer, 3.LMDirichlet, 4. DFR";
    echo "6. [param1]: 'k1'-BM25; lambda-LMJM; mu-LMDi; BasicModel-DFR";
    echo -e "\tDFR(BasicModel) - 1-BE, 2-D, 3-G, 4-IF, 5-In, 6-Ine, 7-P";
    echo "7. [param2]: optional 'b' - BM25; AfterEffect - DFR";
    echo -e "\tDFR(AfterEffect) - 1-B, 2-L";
    echo "8. [param3]: optional 'b' - BM25; Norma. - DFR";
    echo -e "\tDFR(Norma.) - 1-H1, 2-H2, 3-H3, 4-Z, 5-NoNorm";
    exit 1;
fi

indexPath=$1
fieldToSearch=$2
queryPath=$3
qff=$4
sf=$5

prop_name="webdoc-searcher.properties"
#echo $prop_name

cd build/classes

# making the .properties file
cat > $prop_name << EOL

stopFilePath=$stopFilePath

indexPath=$indexPath

queryPath=$queryPath

## queryFields
# 1: title
# 2: title + desc
# 3. title + desc + narr
queryFieldFlag=$qff

## Field to search in the index
fieldToSearch=$fieldToSearch

resPath=$resPath

### Similarity functions:
#0 - DefaultSimilarity
#1 - BM25Similarity
#2 - LMJelinekMercerSimilarity
#3 - LMDirichletSimilarity
similarityFunction=$sf

numHits=1000

EOL
if [ $# -ge 6 ]
then
    cat >> $prop_name << EOL
param1=$6
EOL
fi


if [ $# -eq 7 ]
then
    cat >> $prop_name << EOL
param2=$7
EOL
fi

if [ $# -eq 8 ]
then
    cat >> $prop_name << EOL
param3=$8
EOL
fi

java -Xmx1g -cp $CLASSPATH:dist/WebData.jar:./lib/* searcher.WebDocSearcher $prop_name

