/**
 * RM3: Complete;
 * Relevance Based Language Model with query mix.
 * References:
 *      1. Relevance Based Language Model - Victor Lavrenko - SIGIR-2001
 *      2. UMass at TREC 2004: Novelty and HARD - Nasreen Abdul-Jaleel - TREC-2004
 */
package TrueRelevanceFeedback;

import static common.CommonVariables.FIELD_BOW;
import static common.CommonVariables.FIELD_FULL_BOW;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import common.EnglishAnalyzerWithSmartStopword;
import common.TRECQuery;
import common.TRECQueryParser;
import java.util.Map;

/**
 *
 * @author dwaipayan
 */

public class DivergenceTrfPrf {

    Properties      prop;
    String          indexPath;
    String          queryPath;      // path of the query file
    File            queryFile;      // the query file
    String          stopFilePath;
    IndexReader     indexReader;
    IndexSearcher   indexSearcher;
    String          resPath;        // path of the res file
    FileWriter      resFileWriter;  // the res file writer
    FileWriter      baselineFileWriter;  // the res file writer
    int             numHits;      // number of document to retrieveWithExpansionTermsFromFile
    String          runName;        // name of the run
    List<TRECQuery> queries;
    File            indexFile;          // place where the index is stored
    Analyzer        analyzer;           // the analyzer
    boolean         boolIndexExists;    // boolean flag to indicate whether the index exists or not
    String          fieldToSearch;      // the field in the index to be searched
    String          fieldForFeedback;   // field, to be used for feedback
    TRECQueryParser trecQueryparser;
    int             simFuncChoice;
    float           param1, param2;
    long            vocSize;            // vocabulary size
    EstimateTRLM             rlm;
    Boolean         feedbackFromFile;

    HashMap<String, TopDocs> allTopDocsFromFileHashMap;     // For feedback from file, to contain all topdocs from file
    HashMap<String, String> termsMeta;
    HashMap<String, String> termsClean;

    // +++ TRF
    String qrelPath;
    boolean trf;    // true or false depending on whether True Relevance Feedback is choosen
    HashMap<String, TopDocs> allRelDocsFromQrelHashMap;     // For TRF, to contain all true rel. docs.
    int numTrueRelDoc;
    // --- TRF

    float           mixingLambda;    // mixing weight, used for doc-col weight distribution
    int             numFeedbackTerms;// number of feedback terms
    int             numFeedbackDocs; // number of feedback documents
    float           QMIX;

    public DivergenceTrfPrf(Properties prop) throws IOException, Exception {

        this.prop = prop;
        /* property file loaded */

        // +++++ setting the analyzer with English Analyzer with Smart stopword list
        stopFilePath = prop.getProperty("stopFilePath");
        System.out.println("stopFilePath set to: " + stopFilePath);
        EnglishAnalyzerWithSmartStopword engAnalyzer = new EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        /* index path setting */
        indexPath = prop.getProperty("indexPath");
        System.out.println("indexPath set to: " + indexPath);
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile.toPath());

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexPath);
            boolIndexExists = false;
            System.exit(1);
        }
        fieldToSearch = prop.getProperty("fieldToSearch", FIELD_FULL_BOW);
        fieldForFeedback = prop.getProperty("fieldForFeedback", FIELD_BOW);
        System.out.println("Searching field for retrieval: " + fieldToSearch);
        System.out.println("Field for Feedback: " + fieldForFeedback);
        /* index path set */

        simFuncChoice = Integer.parseInt(prop.getProperty("similarityFunction"));
        if (null != prop.getProperty("param1"))
            param1 = Float.parseFloat(prop.getProperty("param1"));
        if (null != prop.getProperty("param2"))
            param2 = Float.parseFloat(prop.getProperty("param2"));

        /* setting indexReader and indexSearcher */
        indexReader = DirectoryReader.open(FSDirectory.open(indexFile.toPath()));
        indexSearcher = new IndexSearcher(indexReader);
        setSimilarityFunction(simFuncChoice, param1, param2);
        /* indexReader and searher set */

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        System.out.println("queryPath set to: " + queryPath);
        queryFile = new File(queryPath);
        /* query path set */

        /* constructing the query */
        trecQueryparser = new TRECQueryParser(queryPath, analyzer, fieldToSearch);
        queries = constructQueries();
        /* constructed the query */

        // +++ PRF from file
        feedbackFromFile = Boolean.parseBoolean(prop.getProperty("feedbackFromFile"));
        if(feedbackFromFile == true) {
            String feedbackFilePath = prop.getProperty("feedbackFilePath");
            System.out.println("Using feedback information from file: " + feedbackFilePath);
            allTopDocsFromFileHashMap = common.CommonMethods.readTopDocsFromFile(feedbackFilePath, queries, indexReader);
        }
        // --- PRF from file

        // +++ TRF
        qrelPath = prop.getProperty("qrelPath");
        allRelDocsFromQrelHashMap = common.CommonMethods.readRelDocsFromQrel(qrelPath, queries, indexReader);
        // --- TRF

        // numFeedbackTerms = number of top terms to select
        numFeedbackTerms = Integer.parseInt(prop.getProperty("numFeedbackTerms"));
        // numFeedbackDocs = number of top documents to select
        numFeedbackDocs = Integer.parseInt(prop.getProperty("numFeedbackDocs"));

        if(param1>0.99)
            mixingLambda = 0.8f;
        else
            mixingLambda = param1;

        /* setting res path */
        setRunName_ResFileName();
        resFileWriter = new FileWriter(resPath);
        System.out.println("Result will be stored in: "+resPath);

        /* res path set */
        numHits = Integer.parseInt(prop.getProperty("numHits","1000"));
        QMIX = Float.parseFloat(prop.getProperty("rm3.queryMix"));

        rlm = new EstimateTRLM(this);

    }

    /**
     * Sets indexSearcher.setSimilarity() with parameter(s)
     * @param choice similarity function selection flag
     * @param param1 similarity function parameter 1
     * @param param2 similarity function parameter 2
     */
    private void setSimilarityFunction(int choice, float param1, float param2) {

        switch(choice) {
            case 0:
                indexSearcher.setSimilarity(new DefaultSimilarity());
                System.out.println("Similarity function set to DefaultSimilarity");
                break;
            case 1:
                indexSearcher.setSimilarity(new BM25Similarity(param1, param2));
                System.out.println("Similarity function set to BM25Similarity"
                    + " with parameters: " + param1 + " " + param2);
                break;
            case 2:
                indexSearcher.setSimilarity(new LMJelinekMercerSimilarity(param1));
                System.out.println("Similarity function set to LMJelinekMercerSimilarity"
                    + " with parameter: " + param1);
                break;
            case 3:
                indexSearcher.setSimilarity(new LMDirichletSimilarity(param1));
                System.out.println("Similarity function set to LMDirichletSimilarity"
                    + " with parameter: " + param1);
                break;
        }
    } // ends setSimilarityFunction()

    /**
     * Sets runName and resPath variables depending on similarity functions.
     */
    private void setRunName_ResFileName() {

        Similarity s = indexSearcher.getSimilarity(true);
        runName = s.toString()+"-D"+numFeedbackDocs+"-T"+numFeedbackTerms;
        runName += "-rm3-"+Float.parseFloat(prop.getProperty("rm3.queryMix", "0.98"));
        runName += "-" + fieldToSearch + "-" + fieldForFeedback;
        runName = runName.replace(" ", "").replace("(", "").replace(")", "").replace("00000", "");
        if(Boolean.parseBoolean(prop.getProperty("rm3.rerank")) == true)
            runName += "-rerank";
        if(null == prop.getProperty("resPath"))
            resPath = "/home/dwaipayan/";
        else
            resPath = prop.getProperty("resPath");
        resPath = resPath+queryFile.getName()+"-"+runName + ".res";
    } // ends setRunName_ResFileName()

    /**
     * Parses the query from the file and makes a List<TRECQuery> 
     *  containing all the queries (RAW query read)
     * @return A list with the all the queries
     * @throws Exception 
     */
    private List<TRECQuery> constructQueries() throws Exception {

        trecQueryparser.queryFileParse();
        return trecQueryparser.queries;
    } // ends constructQueries()

    public void retrieveAll() throws Exception {

        ScoreDoc[] hits;
        TopDocs trfTopDocs;
        TopDocs topDocs;
        TopScoreDocCollector collector;
        float jsDivergence = 0;
        int qCount = 0;
//        FileWriter baselineRes = new FileWriter(resPath+".baseline");

        System.out.println(allRelDocsFromQrelHashMap.size());
        for (TRECQuery query : queries) {
            collector = TopScoreDocCollector.create(numHits);
            Query luceneQuery = trecQueryparser.getAnalyzedQuery(query);

            System.out.println(query.qid+": Initial query: " + luceneQuery.toString(fieldToSearch));

            // +++ TRF
            System.out.println("TRF from qrel");
            if(null == (trfTopDocs = allRelDocsFromQrelHashMap.get(query.qid))) {
                System.err.println("Error: Query id: "+query.qid+
                    " not present in qrel file");
                continue;
            }
            numTrueRelDoc = trfTopDocs.scoreDocs.length;

            rlm.setFeedbackStats(trfTopDocs, luceneQuery.toString(fieldToSearch).split(" "), this, true);
            /**
             * HashMap of P(w|R) for 'numFeedbackTerms' terms with top P(w|R) among each w in R,
             * keyed by the term with P(w|R) as the value.
             */
            HashMap<String, WordProbability> hashmap_TruePwGivenR;
            //hashmap_PwGivenR = rlm.RM1(query, topDocs);
            hashmap_TruePwGivenR = rlm.RM3(query, trfTopDocs);
            // --- TRF

            // +++ PRF
            // initial retrieval performed
            indexSearcher.search(luceneQuery, collector);
            topDocs = collector.topDocs();
            // --- PRF

            rlm.setFeedbackStats(topDocs, luceneQuery.toString(fieldToSearch).split(" "), this, false);
            /**
             * HashMap of P(w|R) for 'numFeedbackTerms' terms with top P(w|R) among each w in R,
             * keyed by the term with P(w|R) as the value.
             */
            HashMap<String, WordProbability> hashmap_PwGivenR;
            //hashmap_PwGivenR = rlm.RM1(query, topDocs);
            hashmap_PwGivenR = rlm.RM3(query, topDocs);

            // +++ Now compute the JS-Div(hashmap_TruePwGivenR, hashmap_PwGivenR)
            double score = 0;

            score = jsDiv(hashmap_TruePwGivenR, hashmap_PwGivenR);
            jsDivergence += score;
            qCount++;

            System.out.println("Score: " + score);
            // ---
        } // ends for each query
        System.out.println("JS-Divergence: " + jsDivergence/(float)qCount);
    } // ends retrieveAll

    /**
     * Merges two relevance models that are stored in two hashmaps.
     * @param hashmap_TruePwGivenR
     * @param hashmap_PwGivenR
     * @return 
     */
    public HashMap<String, WordProbability> mergeModels(HashMap<String, WordProbability> hashmap_TruePwGivenR, 
        HashMap<String, WordProbability> hashmap_PwGivenR) {

        HashMap<String, WordProbability> mergedAvgModel = new HashMap<>();

        for (Map.Entry<String, WordProbability> entrySet : hashmap_TruePwGivenR.entrySet()) {
            String key = entrySet.getKey();
            WordProbability value = entrySet.getValue();
            mergedAvgModel.put(key, new WordProbability(key, value.p_w_given_R/2));
        }

        for (Map.Entry<String, WordProbability> entrySet : hashmap_PwGivenR.entrySet()) {
            String key = entrySet.getKey();
            WordProbability value = entrySet.getValue();
            WordProbability existingValue = mergedAvgModel.get(key);
            float newWeight = value.p_w_given_R / 2;

            if(null != existingValue) {
                newWeight += existingValue.p_w_given_R;
            }
            mergedAvgModel.put(key, new WordProbability(key, newWeight));
        }

        return mergedAvgModel;
    }

    /**
     * KL Divergence
     * @param dist1
     * @param dist2
     * @return 
     */
    public float klDiv(HashMap<String, WordProbability> dist1, HashMap<String, WordProbability> dist2) {

        float score = 0;
        float proba1, proba2;
        WordProbability wp2;
        String w;

        for (Map.Entry<String, WordProbability> entrySet : dist1.entrySet()) {
        // for each of the words of True Relevant Documents:
            w = entrySet.getKey();
            proba1 = entrySet.getValue().p_w_given_R;
            wp2 = dist2.get(w);
            if(null != wp2) {
                proba2 = wp2.p_w_given_R;
                score += (proba1 * (double) Math.log(proba1 / proba2));
//                System.out.println(proba1 + " " + proba2);
            }
            else {
                System.err.println("The Kullback–Leibler divergence is defined only if Q(i)=0 implies P(i)=0");
            }
        } // ends for each t of True Relevant Documents

        return score;
    }

    /**
     * JS Divergence
     * @param dist1
     * @param dist2
     * @return 
     */
    public float jsDiv(HashMap<String, WordProbability> dist1, HashMap<String, WordProbability> dist2) {

        HashMap<String, WordProbability> avgModel = mergeModels(dist1, dist2);

        float score1 = klDiv(dist1, avgModel);
        float score2 = klDiv(dist2, avgModel);
        return (score1 + score2)/2;
    }

    public static void main(String[] args) throws IOException, Exception {

//        args = new String[1];
//        args[0] = "/home/dwaipayan/Dropbox/programs/Wt10g_processing/WebData/rblm.D-10.T-70.properties";

        String usage = "java RelevanceBasedLanguageModel <properties-file>\n"
            + "Properties file must contain the following fields:\n"
            + "1. stopFilePath: path of the stopword file\n"
            + "2. fieldToSearch: field of the index to be searched\n"
            + "3. indexPath: Path of the index\n"
            + "4. queryPath: path of the query file (in proper xml format)\n"
            + "5. numFeedbackTerms: number of feedback terms to use\n"
            + "6. numFeedbackDocs: number of feedback documents to use\n"
            + "7. [numHits]: default-1000 - number of documents to retrieve\n"
            + "8. rm3.queryMix (0.0-1.0): query mix to weight between P(w|R) and P(w|Q)\n"
            + "9. [rm3.rerank]: default-0 - 1-Yes, 0-No\n"
            + "10. resPath: path of the folder in which the res file will be created\n"
            + "11. similarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity\n"
            + "12. param1: \n"
            + "13. [param2]: optional if using BM25\n";

        Properties prop = new Properties();

        if(1 != args.length) {
            System.out.println("Usage: " + usage);
            args = new String[1];
            args[0] = "rblm.D-20.T-70.properties";
            args[0] = "trblm-T-100.properties";
//            System.exit(1);
        }
        prop.load(new FileReader(args[0]));
        DivergenceTrfPrf rblm = new DivergenceTrfPrf(prop);

        rblm.retrieveAll();
    } // ends main()

}
