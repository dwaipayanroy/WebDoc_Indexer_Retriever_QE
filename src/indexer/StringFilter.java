/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author dwaipayan
 */
public class StringFilter {
    
    static public String filterWebText(String text) {

        String urlPatternStr = "\\b((https?|ftp|file)://|www)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        String tagPatternStr = "<[^>\\n]*[>\\n]";

        Pattern webFilter = Pattern.compile(tagPatternStr+"|"+urlPatternStr);
        Matcher matcher = webFilter.matcher(text);

        return matcher.replaceAll(" ");
    }

    /**
     * Removes the HTML tags from 'str' and returns the resultant string
     * @param str
     * @return 
     */
    static public String removeHTMLTags(String str) {

        String tagPatternStr = "<[^>\\n]*[>\\n]";
        Pattern tagPattern = Pattern.compile(tagPatternStr);

        Matcher m = tagPattern.matcher(str);

        return m.replaceAll(" ");
    }

    /**
     * Removes URLs from 'str' and returns the resultant string
     * @param str
     * @return 
     */
    static public String removeURL(String str) {

        String urlPatternStr = "\\b((https?|ftp|file)://|www)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern urlPattern = Pattern.compile(urlPatternStr);

        Matcher m = urlPattern.matcher(str);

        return m.replaceAll(" ");
    }

    static String readFile(String path, Charset encoding) 
      throws IOException 
    {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, encoding);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        String content;
        String filePath = "/home/dwaipayan/ClueWeb09_English_Sample_File.warc";
//        content = new Scanner(new File(filePath)).useDelimiter("\\Z").next();

        long startTime, endTime, totalTime;
        content = readFile(filePath, StandardCharsets.UTF_8);
        String content1;
//        /*
        startTime = System.currentTimeMillis();
        content1 = removeHTMLTags(content);
        content1 = removeURL(content1);
        endTime   = System.currentTimeMillis();
        totalTime = endTime - startTime;
        System.out.println(totalTime);
        //*/
        
        /*
        startTime = System.currentTimeMillis();
        String content2 = filterWebText(content);
        endTime   = System.currentTimeMillis();
        totalTime = endTime - startTime;
        System.out.println(totalTime);
//        if (content1.equals(content2))
//            System.out.println("Nice!");
//        System.out.println(content);
        //*/
    }
}
