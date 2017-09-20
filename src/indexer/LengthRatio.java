/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 *
 * @author dwaipayan
 */
public class LengthRatio {
    public static void main(String args[]) throws Exception {
	FileInputStream fis = new FileInputStream(new File("/home/dwaipayan/clueweb09b-doclen-full-clean.log"));
        double dc = 0;
        double full = 0, clean = 0;
        double avg = 0.0;

	//Construct BufferedReader from InputStreamReader
	BufferedReader br = new BufferedReader(new InputStreamReader(fis));
 
	String line = null;
	while ((line = br.readLine()) != null) {
            dc++;
            avg += ((full-clean)/full*100.0);
            if(dc%100000==0)
                System.out.println(dc);
	}
 
        System.out.println(avg + " " + dc + " " +avg/dc);
	br.close();
    }
}
