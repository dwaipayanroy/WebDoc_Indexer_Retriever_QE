
package common;

import RelevanceFeedback.NewScore;
import static common.CommonVariables.FIELD_ID;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

/**
 *
 * @author dwaipayan
 */
public class CommonMethods {

    /**
     * Returns a string-buffer in the TREC-res format for the passed queryId
     * @param queryId
     * @param hits
     * @param searcher
     * @param runName
     * @return
     * @throws IOException 
     */
    static final public StringBuffer writeTrecResFileFormat(String queryId, ScoreDoc[] hits, 
        IndexSearcher searcher, String runName) throws IOException {

        StringBuffer resBuffer = new StringBuffer();
        int hits_length = hits.length;
        for (int i = 0; i < hits_length; ++i) {
            int luceneDocId = hits[i].doc;
            Document d = searcher.doc(luceneDocId);
            resBuffer.append(queryId).append("\tQ0\t").
                append(d.get("docid")).append("\t").
                append((i)).append("\t").
                append(hits[i].score).append("\t").
                append(runName).append("\n");                
        }

        return resBuffer;
    }
    /**
     * Read 6 column TREC-res file to use for Relevance feedback 
     * @param resFile The path of the result file
     * @return A hashmap, keyed by the query-id with value, containing the topDocs read from file
     * @throws Exception 
     */
    public static HashMap<String, TopDocs> readTopDocsFromFile(String resFile, List<TRECQuery> queries,
        IndexReader indexReader) throws Exception {

        HashMap<String, TRECQuery> hm_Query = new HashMap();
        for (TRECQuery query : queries) {
            hm_Query.put(query.qid, query);
        }

        HashMap<String, TopDocs> allTopDocsHashMap = new HashMap<>();

        IndexSearcher docidSearcher;
        docidSearcher = new IndexSearcher(indexReader);

        ScoreDoc[] docidHits = null;
        TopDocs docidTopDocs = null;
        Query docidQuery;
        TopScoreDocCollector collector;

        QueryParser docidSearchParser = new QueryParser(FIELD_ID, new WhitespaceAnalyzer());
        BufferedReader br = new BufferedReader(new FileReader(resFile));

        String line;
        String presentQueryId;
        String presentDocId;
        float presentScore;
        int presentLuceneDocId;

        String lastQid = null;

        String tokens[];
        List<NewScore> listLuceneDocId;
        listLuceneDocId = new ArrayList<>();

        do {
            line = br.readLine();

            if(line == null && listLuceneDocId != null) {  // end of file is reached and there are entires in listLuceneDocId to be put in hashmap
                
                ScoreDoc scoreDoc[] = new ScoreDoc[listLuceneDocId.size()];
                for (int i=0; i<listLuceneDocId.size(); i++) {
                    scoreDoc[i] = new ScoreDoc(listLuceneDocId.get(i).luceneDocid, (float) listLuceneDocId.get(i).score);
                }
                TopDocs topDocs;
                topDocs = new TopDocs(listLuceneDocId.size(), scoreDoc, (float) listLuceneDocId.get(0).score);
                TRECQuery trecQuery = hm_Query.get(lastQid);
                System.out.println(lastQid+": "+trecQuery.qtitle);

                allTopDocsHashMap.put(lastQid, topDocs);

                break;
            }

            tokens = line.split("\\t");
            presentQueryId = tokens[0];
            presentDocId = tokens[2];
            presentScore = Float.parseFloat(tokens[4]);

            docidQuery = docidSearchParser.parse(presentDocId);
            collector = TopScoreDocCollector.create(1);
            docidSearcher.search(docidQuery, collector);
            docidTopDocs = collector.topDocs();
            docidHits = docidTopDocs.scoreDocs;
            if(docidHits == null) {
                System.err.println("Lucene docid not found for: "+tokens[2]);
                continue;
            }
            presentLuceneDocId = docidHits[0].doc;

            if(null != lastQid && !lastQid.equals(presentQueryId)) {

                ScoreDoc scoreDoc[] = new ScoreDoc[listLuceneDocId.size()];
                for (int i=0; i<listLuceneDocId.size(); i++) {
                    scoreDoc[i] = new ScoreDoc(listLuceneDocId.get(i).luceneDocid, (float) listLuceneDocId.get(i).score);
                }
                ScoreDoc[] hits;
                TopDocs topDocs;
                topDocs = new TopDocs(listLuceneDocId.size(), scoreDoc, (float) listLuceneDocId.get(0).score);
                TRECQuery trecQuery = hm_Query.get(lastQid);
                System.out.println(lastQid+": "+trecQuery.qtitle);

                allTopDocsHashMap.put(lastQid, topDocs);

                listLuceneDocId = new ArrayList<>();
            }

            listLuceneDocId.add(new NewScore(presentLuceneDocId, presentScore));
            lastQid = presentQueryId;

        } while (true);

        return allTopDocsHashMap;
    } // ends readTopDocsFromFile()

    /**
     * Read the qrel file into an HashMap and return. 
     * @param qrelFile Path of the qrel file
     * @return A HashMap with qid as Key and QueryKnownRel as value.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static HashMap<String, QueryKnownRel> readQrelFile(String qrelFile) throws FileNotFoundException, IOException {

        HashMap<String, QueryKnownRel> allKnownJudgement = new HashMap();

        FileInputStream fis = new FileInputStream(new File(qrelFile));

	BufferedReader br = new BufferedReader(new InputStreamReader(fis));

        String lastQid = "";
        QueryKnownRel singleQueryInfo = new QueryKnownRel();
	String line = null;

        while ((line = br.readLine()) != null) {
            String qid, docid;
            int rel;
            String tokens[] = line.split("[\\s]+");
            qid = tokens[0];
            docid = tokens[2];
            rel = Integer.parseInt(tokens[3]);

            if(lastQid.equals(qid)) {
                if(rel <= 0)
                    singleQueryInfo.nonrelevant.add(docid);
                else
                    singleQueryInfo.relevant.add(docid);
            }
            else {  // a new query is read
                if(!lastQid.isEmpty()) // information about a query is there
                    allKnownJudgement.put(lastQid, singleQueryInfo);

                singleQueryInfo = new QueryKnownRel();
                if(rel <= 0)
                    singleQueryInfo.nonrelevant.add(docid);
                else
                    singleQueryInfo.relevant.add(docid);
                lastQid = qid;
            }
	}

        // for the last query
        allKnownJudgement.put(lastQid, singleQueryInfo);

	br.close();

        return allKnownJudgement;
    } // ends readQrelFile

    public static HashMap<String, TopDocs> readRelDocsFromQrel(String qrelPath, List<TRECQuery> queries,
        IndexReader indexReader) throws Exception {

        HashMap<String, TRECQuery> hm_Query = new HashMap();
        for (TRECQuery query : queries) {
            hm_Query.put(query.qid, query);
        }

        IndexSearcher docidSearcher;
        docidSearcher = new IndexSearcher(indexReader);

        ScoreDoc[] docidHits = null;
        TopDocs docidTopDocs = null;
        Query docidQuery;
        TopScoreDocCollector collector;

        QueryParser docidSearchParser = new QueryParser(FIELD_ID, new WhitespaceAnalyzer());

        HashMap<String, QueryKnownRel> allKnownJudgement;       // For TRF, to contain info about all known relevance

        allKnownJudgement = readQrelFile(qrelPath);         // all known judgements are read

        HashMap<String, TopDocs> allRelDocsHashMap = new HashMap<>();

        for (Map.Entry<String, TRECQuery> entrySet : hm_Query.entrySet()) { // For each query:
            String qid = entrySet.getKey();
//            System.out.println("Query ID: " + qid);
            QueryKnownRel qKnownRel = allKnownJudgement.get(qid);

            int numRel = qKnownRel.relevant.size();
//            System.out.println("NumRel = " + numRel);
            ScoreDoc scoreDoc[] = new ScoreDoc[numRel];

            int notFoundCount = 0;
            for (int i=0; i<numRel; i++) {        // For each true relevant doc for that query:
                String presentDocid = qKnownRel.relevant.get(i);
                docidQuery = docidSearchParser.parse(presentDocid);
                collector = TopScoreDocCollector.create(1);
                docidSearcher.search(docidQuery, collector);
                docidTopDocs = collector.topDocs();
                docidHits = docidTopDocs.scoreDocs;
                if(docidHits.length == 0) {
                    System.err.println("Lucene docid not found for: "+presentDocid);
                    notFoundCount++;
                    continue;
                }
                int presentLuceneDocId = docidHits[0].doc;
                scoreDoc[i] = new ScoreDoc(presentLuceneDocId, 0);
            }
            if(numRel-notFoundCount <=0){
                System.out.println("??");
                char ch = (char) System.in.read();
            }
            TopDocs topDocs = new TopDocs(numRel-notFoundCount, scoreDoc, 0);
            allRelDocsHashMap.put(qid, topDocs);
        }

        return allRelDocsHashMap;
    }

    /**
     * Analyzes 'text', using 'analyzer', to be stored in 'fieldName'.
     * @param analyzer The analyzer to be used for analyzing the text
     * @param text The text to be analyzed
     * @param fieldName The name of the field in which the text is going to be stored
     * @return The analyzed text as StringBuffer
     * @throws IOException 
     */
    public static StringBuffer analyzeText(Analyzer analyzer, String text, String fieldName) throws IOException {

        // +++ For replacing characters- ':','_'
        Map<String, String> replacements = new HashMap<String, String>() {{
            put(":", " ");
            put("_", " ");
        }};
        // create the pattern joining the keys with '|'
        String regExp = ":|_";
        Pattern p = Pattern.compile(regExp);
        // --- For replacing characters- ':','_'

        StringBuffer temp;
        Matcher m;
        StringBuffer tokenizedContentBuff;
        TokenStream stream;
        CharTermAttribute termAtt;

        // +++ For replacing characters- ':','_'
        temp = new StringBuffer();
        m = p.matcher(text);
        while (m.find()) {
            String value = replacements.get(m.group(0));
            if(value != null)
                m.appendReplacement(temp, value);
        }
        m.appendTail(temp);
        text = temp.toString();
        // --- For replacing characters- ':','_'

        tokenizedContentBuff = new StringBuffer();

        stream = analyzer.tokenStream(fieldName, new StringReader(text));
        termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            if(!term.equals("nbsp") && !term.equals("amp"))
                tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff;
    }

    /**
     * Analyzes 'text', using 'analyzer', to be stored in a  dummy field.
     * @param analyzer
     * @param text
     * @return
     * @throws IOException 
     */
    public static StringBuffer analyzeText(Analyzer analyzer, String text) throws IOException {

        StringBuffer temp;
        Matcher m;
        StringBuffer tokenizedContentBuff;
        TokenStream stream;
        CharTermAttribute termAtt;

        tokenizedContentBuff = new StringBuffer();

        stream = analyzer.tokenStream("dummy_field", new StringReader(text));
        termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff;
    }

    // for unit testing
    public static void main(String[] args) throws IOException {

        CommonMethods obj = new CommonMethods();

        // +++ testing readQrelFile()
        HashMap<String, QueryKnownRel> allKnownJudgement = obj.readQrelFile("/home/dwaipayan/Dropbox/ir/corpora-stats/qrels/cw09b.qrel");
        for (Map.Entry<String, QueryKnownRel> entrySet : allKnownJudgement.entrySet()) {
            String key = entrySet.getKey();
            QueryKnownRel value = entrySet.getValue();
            System.out.println(key + " " + value.toString(1));
        }
        // --- testing readQrelFile()

    }
}

