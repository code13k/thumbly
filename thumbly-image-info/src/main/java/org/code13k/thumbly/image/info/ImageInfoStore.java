package org.code13k.thumbly.image.info;

import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.image.info.model.ImageInfo;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class ImageInfoStore {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(ImageInfoStore.class);

    // Const
    public static final String DATA_STORE_FILE_NAME = "image_info.db";

    // Data
    private DB mDB = null;
    private ConcurrentMap<String, ImageInfo> mData = null;

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final ImageInfoStore INSTANCE = new ImageInfoStore();
    }

    public static ImageInfoStore getInstance() {
        return ImageInfoStore.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private ImageInfoStore() {
        mLogger.trace("ImageInfoStore()");
    }

    /**
     * Constructor
     */
    synchronized public void init(String baseDirectory, int expirationHours) {
        if (mDB == null) {
            try {
                if (Files.exists(Paths.get(baseDirectory)) == false) {
                    Files.createDirectories(Paths.get(baseDirectory));
                }
                mDB = DBMaker
                        .fileDB(baseDirectory + "/" + DATA_STORE_FILE_NAME)
                        .closeOnJvmShutdown()
                        .fileMmapEnable()
                        .fileMmapEnableIfSupported()
                        .make();
                mData = (ConcurrentMap<String, ImageInfo>) mDB
                        .hashMap("ImageInfoStore")
                        .expireAfterCreate(expirationHours, TimeUnit.HOURS)
                        .expireAfterGet(expirationHours, TimeUnit.HOURS)
                        .createOrOpen();

                // Log
                if (mLogger.isTraceEnabled() == true) {
                    mLogger.trace("------------------------------------------------------------------------");
                    mLogger.trace("Cached Image ImageInfo");
                    mLogger.trace("------------------------------------------------------------------------");
                    mLogger.trace("Size = " + mData.size());
                    mData.forEach((key, value) -> mLogger.trace(key + " = " + value));
                    mLogger.trace("------------------------------------------------------------------------");
                }
            } catch (Exception e) {
                mLogger.error("Failed to initialize", e);
            }
        } else {
            mLogger.debug("Duplicated initializing");
        }
    }

    /**
     * Set
     */
    public boolean set(String key, ImageInfo value) {
        if (StringUtils.isEmpty(key) == true) {
            return false;
        }
        if (value == null) {
            return false;
        }
        mData.put(key, value);
        mDB.commit();
        return true;
    }

    /**
     * Get
     */
    public ImageInfo get(String key) {
        if (StringUtils.isEmpty(key) == true) {
            return null;
        }
        ImageInfo result = mData.get(key);
        return result;
    }

    /**
     * Size
     */
    public int size(){
        return mData.size();
    }
}
