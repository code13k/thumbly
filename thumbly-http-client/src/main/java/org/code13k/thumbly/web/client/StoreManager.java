package org.code13k.thumbly.web.client;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;

public class StoreManager {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(StoreManager.class);

    // Data
    private Long mMaxSizeOfCacheDirectory = null;

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final StoreManager INSTANCE = new StoreManager();
    }

    public static StoreManager getInstance() {
        return StoreManager.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private StoreManager() {
        mLogger.trace("StoreManager()");
    }

    /**
     * Initialize
     */
    synchronized public void init(long maxSizeOfCacheDirectory) {
        if (mMaxSizeOfCacheDirectory == null) {
            mMaxSizeOfCacheDirectory = maxSizeOfCacheDirectory;
            runStoreCleaner();
        } else {
            mLogger.debug("Duplicated initializing");
        }
    }

    /**
     * Cleaning
     */
    private void cleaning() {
        // Get cache directory
        String cacheDirectory = FileStore.getInstance().getCacheDirectory();
        mLogger.trace("cacheDirectory = " + cacheDirectory);

        // Get size of cache directory
        long sizeOfCacheDirectory = Util.sizeOfCacheDirectory(cacheDirectory);
        mLogger.trace("sizeOfCacheDirectory = " + sizeOfCacheDirectory);
        String displaySize = FileUtils.byteCountToDisplaySize(sizeOfCacheDirectory);
        mLogger.info("Size of cache directory # " + sizeOfCacheDirectory + "byte (" + displaySize + ")");

        // Check if size of cache directory exceed max size
        if (sizeOfCacheDirectory > mMaxSizeOfCacheDirectory) {
            mLogger.info("Cache directory is full # " + sizeOfCacheDirectory + " (" + mMaxSizeOfCacheDirectory + ")");

            // Data
            long additionalSize = (long) (mMaxSizeOfCacheDirectory * 0.1);
            long remainSize = sizeOfCacheDirectory - mMaxSizeOfCacheDirectory;
            remainSize += additionalSize;

            // Delete old cache files
            mLogger.debug("------------------------------------------------------------------------");
            mLogger.debug("Deleted Cache Files");
            mLogger.debug("------------------------------------------------------------------------");
            int fileDeleted = 0;
            do {
                ArrayList<String> filenameList = Util.getFilesOrderByAccessTime(cacheDirectory, 100);
                for (int i = 0; i < filenameList.size(); i++) {
                    String filename = filenameList.get(i);
                    String extension = FilenameUtils.getExtension(filename);
                    if (extension.equalsIgnoreCase("db") == false) {
                        mLogger.debug(filename);
                        File file = new File(cacheDirectory + "/" + filename);
                        long fileSize = file.length();
                        if (true == file.delete()) {
                            WebDataStore.getInstance().delete(filename);
                            fileDeleted++;
                            remainSize = remainSize - fileSize;
                            if (remainSize < 0) {
                                break;
                            }
                        }
                    }
                }
            } while (remainSize > 0);
            mLogger.debug("------------------------------------------------------------------------");
            mLogger.info("Deleted oldest cache files # " + fileDeleted);
        }
    }

    /**
     * Run store-cleaner
     */
    private void runStoreCleaner() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        cleaning();

                        // Sleep
                        try {
                            Thread.sleep(1000 * 60 * 10);
                        } catch (Exception e) {
                            // Nothing
                        }
                    } catch (Exception e) {
                        mLogger.error("executeOperationTimeChecker() : " + e);
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setName("thumbly-web-client-store-cleaner");
        thread.start();
    }
}
