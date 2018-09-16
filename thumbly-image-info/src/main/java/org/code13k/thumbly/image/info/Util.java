package org.code13k.thumbly.image.info;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;

public class Util {

    /**
     * Get processing time (Milli Seconds)
     */
    public static long processingTime(long startTime){
        return System.currentTimeMillis() - startTime;
    }

    /**
     * MD5FromFile()
     */
    public static String MD5FromFile(String path) {
        try {
            FileInputStream fis = new FileInputStream(new File(path));
            String md5 = DigestUtils.md5Hex(fis);
            fis.close();
            return md5;
        } catch (Exception e) {
            // Nothing
        }
        return null;
    }

}
