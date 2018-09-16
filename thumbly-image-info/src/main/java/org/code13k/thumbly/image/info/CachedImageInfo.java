package org.code13k.thumbly.image.info;

import org.code13k.thumbly.image.info.model.ImageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CachedImageInfo {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(CachedImageInfo.class);

    // Data
    private Operator mOperator = null;
    private Map<String, List<Consumer<ImageInfo>>> mRequestMap = null;

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final CachedImageInfo INSTANCE = new CachedImageInfo();
    }

    public static CachedImageInfo getInstance() {
        return CachedImageInfo.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private CachedImageInfo() {
        mLogger.trace("CachedImageInfo()");
    }

    /**
     * Initialize
     */
    synchronized public void init(String cacheDirectory, int expirationHours, int operatorCount) {
        if (mOperator == null) {
            ImageInfoStore.getInstance().init(cacheDirectory, expirationHours);
            mOperator = new Operator(operatorCount);
            mRequestMap = new HashMap<>();
        } else {
            mLogger.debug("Duplicated initializing");
        }
    }

    /**
     * Get image info
     */
    public void get(String filePath, Consumer<ImageInfo> consumer) {
        final String key = Util.MD5FromFile(filePath);
        ImageInfo imageInfo = ImageInfoStore.getInstance().get(key);
        mLogger.trace("key = " + key);

        /**
         * Cache HIT!!
         */
        if (imageInfo != null) {
            mLogger.debug("Cache HIT!!");
            consumer.accept(imageInfo);
            return;
        }

        /**
         * Cache Not HIT!!
         */
        // Check if duplicate request is already running
        synchronized (mRequestMap) {
            List<Consumer<ImageInfo>> consumerList;
            if (mRequestMap.containsKey(key)) {
                consumerList = mRequestMap.get(key);
                consumerList.add(consumer);
                return;
            } else {
                consumerList = new ArrayList<>();
                consumerList.add(consumer);
                mRequestMap.put(key, consumerList);
            }
        }

        // Operate
        mOperator.operate(filePath, new Consumer<ImageInfo>() {
            @Override
            public void accept(ImageInfo imageInfo) {
                mLogger.debug("key = " + key);
                mLogger.debug("Image ImageInfo = " + imageInfo.toString());
                ImageInfoStore.getInstance().set(key, imageInfo);

                // End
                List<Consumer<ImageInfo>> consumerList = null;
                synchronized (mRequestMap) {
                    if (mRequestMap.containsKey(key)) {
                        consumerList = new ArrayList<>(mRequestMap.get(key));
                        mRequestMap.remove(key);
                    } else {
                        mLogger.error("Your algorithm is wrong. It's very critical.");
                    }
                }
                if (consumerList != null) {
                    mLogger.debug("Duplicate request : " + consumerList.size());
                    consumerList.forEach(consumerItem -> {
                        consumerItem.accept(imageInfo);
                    });
                }
            }
        });
    }

    /**
     * Get cached image info
     */
    public ImageInfo getCached(String filePath){
        final String key = Util.MD5FromFile(filePath);
        ImageInfo imageInfo = ImageInfoStore.getInstance().get(key);
        mLogger.trace("key = " + key);

        if (imageInfo != null) {
            return imageInfo;
        }
        return null;
    }

    /**
     * Size of cached info
     */
    public int size(){
        return ImageInfoStore.getInstance().size();
    }
}
