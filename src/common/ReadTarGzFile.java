
package common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 *
 * @author dwaipayan
 */
public class ReadTarGzFile {

    public static void uncompressTarGZ(File tarFile, File dest) throws IOException {

        dest.mkdir();
        TarArchiveInputStream tarIn = null;

        tarIn = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(tarFile))));

        TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
        // tarIn is a TarArchiveInputStream
        while (tarEntry != null) {// create a file with the same name as the tarEntry
//        tarEntry.setName("/home/dwaipayan/Downloads/gov2.sample/1");
            File destPath = new File(dest, tarEntry.getName().replaceAll("/", "-"));
//            System.out.println("working: " + destPath.getCanonicalPath());
            System.out.println("working: " + tarEntry.getName());
//            if (tarEntry.isDirectory()) {
//                destPath.mkdirs();
//            } else 
            if(!tarEntry.isDirectory())
            {
                destPath = new File(dest, tarEntry.getName().replaceAll("/", "-"));
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
                    bout.write(btoRead,0,len);
                }

                bout.close();
                btoRead = null;

            }
            tarEntry = tarIn.getNextTarEntry();
//            char ch = (char) System.in.read();
//            destPath.delete();
//            System.out.println("deleted");
//            ch = (char) System.in.read();
        }
        tarIn.close();
    } 

    // For unit testing
    public static void main(String[] args) throws IOException {
        uncompressTarGZ(new File("/home/dwaipayan/Desktop/gov2.sample.tar.gz"), new File("/home/dwaipayan/Downloads/gov2.sample"));
    }

}
