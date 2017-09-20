/**
 * Incomplete.
 * WebDocIndexer.java
 * It indexes:
 *      1 - content (only the text part, excluding tags) FOR FEEDBACK
 *      2 - fullContent of the document (including tags) FOR 1st level Retrieval
 */

package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import static common.CommonVariables.FIELD_ID;
import common.EnglishAnalyzerWithSmartStopword;
import common.WebDocAnalyzer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author dwaipayan
 */
public class WebDocIndexer {
    
    String      propPath;
    Properties  prop;               // prop of the init.properties file

    String      collPath;           // path of the collection
    String      collSpecPath;       // path of the collection spec file

    // for indexing from tar archive
    boolean     boolIndexFromTar;   // boolean flag to indicate whether to index from tar (True), or not (False)
    String      collTarPath;
    File        tarFile;            // the tar file
    String      tempDestPath;       // temporary destiantion path to extract the files from the tar archive
    File        tempDestFile;       // temporary destination file

    File        collDir;            // collection Directory
    File        indexFile;          // place where the index will be stored
    String      toStore;            // YES / NO; to be read from prop file; default - 'NO'
    String      storeTermVector;    // NO / YES / WITH_POSITIONS / WITH_OFFSETS / WITH_POSITIONS_OFFSETS; to be read from prop file; default - YES
    String      stopFilePath;
    Analyzer    analyzer;           // analyzer
    Analyzer    webDocAnalyzer;           // webDocAnalyzer

    IndexWriter indexWriter;
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done
    boolean     boolToDump;

    WebDocIterator docs;

    /**
     * 
     * @param propPath
     * @throws Exception 
     */
    private WebDocIndexer(String propPath) throws Exception {

        this.propPath = propPath;
        prop = new Properties();
        try {
            prop.load(new FileReader(propPath));
        } catch (IOException ex) {
            System.err.println("Error: prop file missing at: "+propPath);
            System.exit(1);
        }
        // ----- properties file set

        // +++++ setting the analyzer with English Analyzer with Smart stopword list
        EnglishAnalyzerWithSmartStopword engAnalyzer;
        stopFilePath = prop.getProperty("stopFilePath");
        if (null == stopFilePath)
            engAnalyzer = new common.EnglishAnalyzerWithSmartStopword();
        else
            engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer
        webDocAnalyzer = new WebDocAnalyzer();   // Not used

        /* collection path setting */
        if(prop.containsKey("collSpec")) {
            boolIndexFromSpec = true;
            collSpecPath = prop.getProperty("collSpec");
        }
        else if(prop.containsKey("collPath")) {
            boolIndexFromSpec = false;
            collPath = prop.getProperty("collPath");
            collDir = new File(collPath);
            if (!collDir.exists() || !collDir.canRead()) {
                System.err.println("Collection directory '" +collDir.getAbsolutePath()+ "' does not exist or is not readable");
                System.exit(1);
            }
        }
        else if(prop.containsKey("collTar")) {
            boolIndexFromTar = true;
            collTarPath = prop.getProperty("collTar");
            tarFile = new File(collTarPath);
            tempDestPath = prop.getProperty("tempDestPath", "/tmp/gov2-temp/");
            tempDestFile = new File(tempDestPath);
        }
        else {
            System.err.println("Neither collPath nor collSpec or collTar is present");
            System.exit(1);
        }
        /* collection path set */

        /* index path setting */
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile.toPath());
        /* index path set */

        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
        }
        else {
            System.out.println("Creating the index in: " + indexFile.getAbsolutePath());
            boolIndexExists = false;
            // +++++ setting the IndexWriterConfig
            // NOTE: WhitespaceAnalyzer is used as the content will be analyzed 
            //  using previously defined analyzer.
            IndexWriterConfig iwcfg = new IndexWriterConfig(new WhitespaceAnalyzer());
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            // ----- iwcfg set
            indexWriter = new IndexWriter(indexDir, iwcfg);
        }

        // +++ toStore or not
        if(prop.containsKey("toStore")) {
            toStore = prop.getProperty("toStore");
            if(!toStore.equals("YES")&&!toStore.equals("NO")){
                System.err.println("prop file: toStore=YES/NO (case-sensitive); if not specified, considers NO");
                System.exit(1);
            }
        }
        else    // default value
            toStore = "NO";
        // --- 

        // +++ storeTermVector or not
        if(prop.containsKey("storeTermVector")) {
            storeTermVector = prop.getProperty("storeTermVector");

            if(!storeTermVector.equals("YES")&&!storeTermVector.equals("NO")&&
                !storeTermVector.equals("WITH_POSITIONS")&&!storeTermVector.equals("WITH_OFFSETS")&&
                !storeTermVector.equals("WITH_POSITIONS_OFFSETS")) {
                System.err.println("prop file: storeTermVector=NO / YES(default)/ "
                    + "WITH_POSITIONS / WITH_OFFSETS / WITH_POSITIONS_OFFSETS "
                    + "(case-sensitive)");
                System.exit(1);
            }
        }
        else    // default value
            storeTermVector = "YES";
        // --- toStore or not

        dumpPath = null;
        if(prop.containsKey("dumpPath")) {
            boolDumpIndex = true;
            dumpPath = prop.getProperty("dumpPath");
        }

        docs = new WebDocIterator(analyzer, this.toStore, this.dumpPath, prop);
    }

    /**
     * Process the Web documents.
     * @param file 
     */
    private void processWebFile(File file) {

        try {

            docs.setFileToRead(file);

            Document doc;
            while (docs.hasNext()) {
                doc = docs.next();
                if (doc != null) {
                    System.out.println((++docIndexedCounter)+": Indexing doc: " + doc.getField(FIELD_ID).stringValue());
                    indexWriter.addDocument(doc);
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println("Error: '"+file.getAbsolutePath()+"' not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.err.println("Error: IOException on reading '"+file.getAbsolutePath()+"'");
            ex.printStackTrace();
        }

    }

    /**
     * Process the directory containing the collection.
     * @param collDir File, of a directory containing the collection.
     */
    private void processDirectory(File collDir) {

        File[] files = collDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                processDirectory(file);  // recurse
            }
            else {
                processWebFile(file);
            }
        }
    }

    /**
     * 
     * @throws Exception 
     */
    public void createIndex() throws Exception {

        System.out.println("Indexing started");

        if (boolIndexFromSpec) {
        /* if collectiomSpec is present, then index from the spec file */
            System.out.println("Reading collection file path from spec file at: "+collSpecPath);
            try (BufferedReader br = new BufferedReader(new FileReader(collSpecPath))) {
                String line;

                while ((line = br.readLine()) != null) {
                    //System.out.println(line);
                    // each line is a file containing documents
                    processWebFile(new File(line));
                }
            }
        }
        else {
        /* index from collPath, i.e. the actual path of the root directory containing the collection */
            System.out.println("Reading collection considering collPath as root directory");
            if (collDir.isDirectory())
                processDirectory(collDir);
            else
                processWebFile(collDir);
        }

        indexWriter.close();

        System.out.println("Indexing ends\n"+docIndexedCounter + " files indexed");
    }

    public void indexFromTarArchive() throws IOException, Exception {

        if (indexWriter == null ) {
            System.err.println("Index already exists at " + indexFile.getName() + ". Skipping...");
            return;
        }

        System.out.println("Indexing started");

        int bulkFileReadSize = Integer.parseInt(prop.getProperty("bulkFileReadSize", "10")); // number of files to extract from the tar
        int fileCounter = 0; // number of files from archive, to index at a time
        tempDestFile.mkdir();
        TarArchiveInputStream tarIn = null;

        tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarFile))));

        TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
        // tarIn is a TarArchiveInputStream
        while (tarEntry != null) {// create a file with the same name as the tarEntry

            if(!tarEntry.isDirectory())
            {
                //System.out.println("Processing: " + tarEntry.getName());
                File destPath = new File(tempDestFile, tarEntry.getName().replaceAll("/", "-"));
                destPath.createNewFile();
                //byte [] btoRead = new byte[(int)tarEntry.getSize()];
                byte [] btoRead = new byte[1024];
                //FileInputStream fin 
                //  = new FileInputStream(destPath.getCanonicalPath());
                BufferedOutputStream bout = 
                    new BufferedOutputStream(new FileOutputStream(destPath));
                int len = 0;

                while((len = tarIn.read(btoRead)) != -1)
                {
                    bout.write(btoRead, 0, len);
                }

                bout.close();
                btoRead = null;

                if(fileCounter >= bulkFileReadSize) {
                    processDirectory(tempDestFile);
                    FileUtils.deleteDirectory(tempDestFile);
                    tempDestFile.mkdir();
                    fileCounter = 0;
                }
                else
                    fileCounter ++;
            }
            tarEntry = tarIn.getNextTarEntry();

        }
        tarIn.close();

        if(fileCounter > 0) {
            processDirectory(tempDestFile);
            FileUtils.deleteDirectory(tempDestFile);
        }

        indexWriter.close();

        System.out.println("Indexing ends.\n"+
            docIndexedCounter + " documents indexed in: " + indexFile.getAbsolutePath());
    }

    public static void main(String[] args) throws Exception {

        WebDocIndexer collIndexer;

        String usage = "Usage: java Wt10gIndexer <init.properties>\n"
        + "Properties file must contain:\n"
        + "1. collSpec = path of the spec file containing the collection spec\n"
        + "2. indexPath = dir. path in which the index will be stored\n"
        + "3. stopFile = path of the stopword list file\n"
        + "4. [OPTIONAL] dumpPath = path of the file to dump the content\n"
        + "5. [OPTIONAL] toStore = YES / NO (default)\n"
        + "6. [OPTIONAL] storeTermVector==NO/YES(default)/"
            + "WITH_POSITIONS/WITH_OFFSETS/WITH_POSITIONS_OFFSETS";

        // for debuging purpose
        /*
        args = new String[1];
        args[0] = "build/classes/webdoc-indexer.properties";
        //*/
        if(args.length == 0) {
            System.out.println(usage);
            System.exit(1);
        }

        collIndexer = new WebDocIndexer(args[0]);

        if(collIndexer.boolIndexExists == false) {
            if(collIndexer.boolIndexFromTar)
                collIndexer.indexFromTarArchive();                
            else
                collIndexer.createIndex();
            collIndexer.boolIndexExists = true;
        }
    }

}
