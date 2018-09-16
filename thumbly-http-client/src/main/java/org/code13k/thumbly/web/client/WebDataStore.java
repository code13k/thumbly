package org.code13k.thumbly.web.client;

import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.web.client.model.WebData;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class WebDataStore {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(WebDataStore.class);

    // Const
    public static final String DATA_STORE_FILE_NAME = "web_data.db";

    // Data
    private DB mDB = null;
    private ConcurrentMap<String, WebData> mData = null;

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final WebDataStore INSTANCE = new WebDataStore();
    }

    public static WebDataStore getInstance() {
        return WebDataStore.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private WebDataStore() {
        mLogger.trace("WebDataStore()");
    }

    /**
     * Constructor
     */
    synchronized public void init(String baseDirectory) {
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
                mData = (ConcurrentMap<String, WebData>) mDB.hashMap("WebDataStore").createOrOpen();

                // Log
                if (mLogger.isTraceEnabled() == true) {
                    mLogger.trace("------------------------------------------------------------------------");
                    mLogger.trace("Cached Web Data");
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
    public boolean set(String key, WebData value) {
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
    public WebData get(String key) {
        if (StringUtils.isEmpty(key) == true) {
            return null;
        }
        return mData.get(key);
    }

    /**
     * Delete
     */
    public void delete(String key) {
        if (StringUtils.isEmpty(key) == true) {
            return;
        }
        mData.remove(key);
        mDB.commit();
    }

    public void delete(List<String> keys) {
        if (keys != null && keys.size() > 0) {
            keys.forEach(key -> mData.remove(key));
            mDB.commit();
        }
    }
}
