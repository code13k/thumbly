package org.code13k.thumbly.lib;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(Util.class);

    /**
     * Calculate processing time
     */
    public static long processingTime(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * toMillis
     * <p>
     * s = seconds
     * m = minuts
     * h = hours
     * d = days
     */
    public static int toSeconds(String humanReadableTime) {
        try {
            String value = StringUtils.substring(humanReadableTime, 0, humanReadableTime.length() - 1);
            String unit = StringUtils.substring(humanReadableTime, humanReadableTime.length() - 1);
            int valueNumber = Integer.valueOf(value);
            int unitNumber = 0;
            int result = 0;

            mLogger.debug("toSeconds() : value=" + value + ", unit=" + unit);
            if (unit.equalsIgnoreCase("s")) {
                unitNumber = 1;
            } else if (unit.equalsIgnoreCase("m")) {
                unitNumber = 60;
            } else if (unit.equalsIgnoreCase("h")) {
                unitNumber = 60 * 60;
            } else if (unit.equalsIgnoreCase("d")) {
                unitNumber = 60 * 60 * 24;
            }
            result = valueNumber * unitNumber;
            return result;
        } catch (Exception e) {
            mLogger.error("toMillis() error : " + e);
            return 0;
        }
    }

    /**
     * toBytes()
     */
    public static long toBytes(String humanReadableSize) {
        long returnValue = -1;
        Pattern patt = Pattern.compile("([\\d.]+)([GMKB])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = patt.matcher(humanReadableSize);
        Map<String, Integer> powerMap = new HashMap<String, Integer>();
        powerMap.put("G", 3);
        powerMap.put("M", 2);
        powerMap.put("K", 1);
        if (matcher.find()) {
            String number = matcher.group(1);
            int pow = powerMap.get(matcher.group(2).toUpperCase());
            BigDecimal bytes = new BigDecimal(number);
            bytes = bytes.multiply(BigDecimal.valueOf(1024).pow(pow));
            returnValue = bytes.longValue();
        }
        return returnValue;
    }

    /**
     * Splits the provided text into an ArrayList, separators specified.
     */
    public static ArrayList<String> splitString(final String string, final String separatorChars) {
        ArrayList<String> result = new ArrayList<String>();
        if (StringUtils.isEmpty(string) == false) {
            String[] tempArray = StringUtils.split(string, separatorChars);
            if (tempArray != null && tempArray.length > 0) {
                for (int i = 0; i < tempArray.length; i++) {
                    String temp = StringUtils.trim(tempArray[i]);
                    result.add(temp);
                }
            }
        }
        return result;
    }


    /**
     * Check if the given number is valid port number.
     */
    public static boolean isValidPortNumber(Integer portNumber) {
        if (portNumber == null || portNumber < 1 || portNumber > 65535) {
            return false;
        }
        return true;
    }

    /**
     * Get app version from manifest info
     */
    public static String getApplicationVersion() {
        Enumeration resourceEnum;
        try {
            resourceEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (resourceEnum.hasMoreElements()) {
                try {
                    URL url = (URL) resourceEnum.nextElement();
                    InputStream is = url.openStream();
                    if (is != null) {
                        Manifest manifest = new Manifest(is);
                        Attributes attr = manifest.getMainAttributes();
                        String version = attr.getValue("Implementation-Version");
                        if (version != null) {
                            return version;
                        }
                    }
                } catch (Exception e) {
                    // Nothing
                }
            }
        } catch (IOException e1) {
            // Nothing
        }
        return null;
    }
}
