package org.code13k.thumbly.business;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;
import com.hazelcast.map.listener.MapClearedListener;
import com.hazelcast.map.listener.MapEvictedListener;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.app.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ClusteredSecretUrl {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(ClusteredSecretUrl.class);

    // Const
    private static final String NAME = "Code13k-Thumbly-Clustered-Secret-Url";

    // Data
    private IMap<String, String> mData = null;

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final ClusteredSecretUrl INSTANCE = new ClusteredSecretUrl();
    }

    public static ClusteredSecretUrl getInstance() {
        return ClusteredSecretUrl.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private ClusteredSecretUrl() {
        mLogger.trace("ClusteredSecretUrl()");
    }

    /**
     * Initialize
     */
    synchronized public void init() {
        if (mData == null) {
            mData = Cluster.getInstance().getHazelcastInstance().getMap(NAME);
            mData.addEntryListener(new MapEvictedListener() {
                @Override
                public void mapEvicted(MapEvent event) {
                    mLogger.debug("mapEvicted # " + event);
                }
            }, true);
            mData.addEntryListener(new MapClearedListener() {
                @Override
                public void mapCleared(MapEvent event) {
                    mLogger.debug("mapCleared # " + event);
                }
            }, true);
        } else {
            mLogger.debug("Duplicated initializing");
        }
    }

    /**
     * Set
     */
    public void set(String originPath, long ttl, Consumer<String> consumer) {
        if (StringUtils.isEmpty(originPath) == false) {
            String secretPath = generateSecretKey(originPath, ttl);
            ICompletableFuture<String> future = mData.putAsync(secretPath, originPath, ttl, TimeUnit.SECONDS);
            future.andThen(new ExecutionCallback<String>() {
                @Override
                public void onResponse(String response) {
                    mLogger.trace("response = " + response);
                    if (consumer != null) {
                        consumer.accept(secretPath);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    mLogger.error("Error occurred", t);
                    if (consumer != null) {
                        consumer.accept(null);
                    }
                }
            });
        } else {
            if (consumer != null) {
                consumer.accept(null);
            }
        }
    }

    /**
     * Get origin path
     */
    public void get(String secretPath, Consumer<String> consumer) {
        if (StringUtils.isEmpty(secretPath) == false) {
            ICompletableFuture<String> future = mData.getAsync(secretPath);
            future.andThen(new ExecutionCallback<String>() {
                @Override
                public void onResponse(String response) {
                    mLogger.trace("response = " + response);
                    if (consumer != null) {
                        consumer.accept(response);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    mLogger.error("Error occurred", t);
                    if (consumer != null) {
                        consumer.accept(null);
                    }
                }
            });
        } else {
            if (consumer != null) {
                consumer.accept(null);
            }
        }
    }

    /**
     * Generate Key
     */
    private String generateSecretKey(String originPath, long expires) {
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        sb.append(originPath);
        sb.append("" + expires);
        sb.append("" + System.currentTimeMillis());
        sb.append("" + System.nanoTime());
        sb.append("" + random.nextInt());
        sb.append("" + random.nextInt());
        sb.append("" + random.nextInt());

        // End
        return DigestUtils.sha512Hex(sb.toString());
    }
}
