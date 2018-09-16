package org.code13k.thumbly.image.processor;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

public class Util {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(Util.class);

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

    /**
     * Get processing time (Milli Seconds)
     */
    public static long processingTime(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Get files order by access time
     */
    public static ArrayList<String> getFilesOrderByAccessTime(String path, int count) {
        String output = execute("ls -tur " + path + " | head -" + count);
        String[] resultArray = StringUtils.split(output, System.lineSeparator());
        ArrayList<String> result = new ArrayList<>();

        for (int i = 0; i < resultArray.length; i++) {
            result.add(resultArray[i]);
        }
        return result;
    }

    /**
     * Get size of cache directory
     */
    public static long sizeOfCacheDirectory(String path) {
        String output = execute("du -k -d 0 " + path);
        String[] resultArray = StringUtils.split(output, "\t");

        // Log
        if (true == mLogger.isTraceEnabled()) {
            for (int i = 0; i < resultArray.length; i++) {
                mLogger.trace("sizeOfCacheDirectory() #" + i + " = " + StringUtils.trim(resultArray[i]));
            }
        }

        // Result
        if (resultArray.length > 1) {
            String resultString = StringUtils.trim(resultArray[0]);
            long result = NumberUtils.toLong(resultString, -1);
            return result * 1024;
        }
        mLogger.error("Failed to parse cache directory # " + path);
        return -1;
    }

    /**
     * Execute process
     */
    public static String execute(String cmd) {
        String[] command = {
                "/bin/sh",
                "-c",
                cmd
        };
        StringBuffer output = new StringBuffer();
        String string = null;

        try {
            Process p = Runtime.getRuntime().exec(command);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            // Standard Output
            while ((string = stdInput.readLine()) != null) {
                output.append("\n");
                output.append(string);
            }
            // Error Output
            while ((string = stdError.readLine()) != null) {
                output.append("\n");
                output.append(string);
            }
        } catch (IOException e) {
            mLogger.error("Exception : " + command + " : ", e);
        }

        return output.toString();
    }

}
