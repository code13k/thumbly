package org.code13k.thumbly.business;

import net.jodah.expiringmap.ExpiringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class SecretUrlManager {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(SecretUrlManager.class);

    // Data
    private ExpiringMap<String, String> mData = null;

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final SecretUrlManager INSTANCE = new SecretUrlManager();
    }

    public static SecretUrlManager getInstance() {
        return SecretUrlManager.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private SecretUrlManager() {
        mLogger.trace("SecretUrlManager()");
    }

    /**
     * Initialize
     */
    synchronized public void init() {
        if (mData == null) {
            mData = ExpiringMap.builder().variableExpiration().build();
        } else {
            mLogger.debug("Duplicated initializing");
        }
    }

    /**
     * Set secret url
     */
    public boolean set(String secretPath, String originPath, long ttl) {
        if (mData.containsKey(secretPath) == true) {
            return false;
        }
        String result = mData.put(secretPath, originPath, ttl, TimeUnit.SECONDS);
        return result == null;
    }

    /**
     * Get origin path
     */
    public String get(String secretPath) {
        return mData.get(secretPath);
    }

    /**
     * Get expiration time
     */
    public long getExpiration(String secretPath) {
        long result = mData.getExpectedExpiration(secretPath);
        if (result > 0) {
            result = result / 1000;
        }
        return result;
    }
}
