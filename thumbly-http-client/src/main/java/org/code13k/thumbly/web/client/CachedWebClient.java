package org.code13k.thumbly.web.client;


import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.web.client.model.WebData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class CachedWebClient {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(CachedWebClient.class);

    // Const
    private static final String DEFAULT_CACHE_DIRECTORY = ".cache";
    private static final long DEFAULT_MAX_SIZE_OF_CACHE_DIRECTORY = 1024 * 1024 * 1024 * 1; // 1GB

    // Data
    private Map<String, List<Consumer<String>>> mRequestMap = null;
    private WebRequest mWebRequest = null;

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final CachedWebClient INSTANCE = new CachedWebClient();
    }

    public static CachedWebClient getInstance() {
        return CachedWebClient.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private CachedWebClient() {
        mLogger.trace("CachedWebClient()");
    }

    /**
     * Initialize
     */
    synchronized public void init(String cacheDirectory, long maxSizeOfCacheDirectory, int eventLoopPoolSize) {
        if (mRequestMap == null) {
            mRequestMap = new HashMap<>();
            mWebRequest = new WebRequest(eventLoopPoolSize);
            FileStore.getInstance().init(cacheDirectory);
            WebDataStore.getInstance().init(cacheDirectory);
            StoreManager.getInstance().init(maxSizeOfCacheDirectory);
        } else {
            mLogger.debug("Duplicated initializing");
        }
    }

    /**
     * Get file
     */
    public void getFile(String url, Map<String, String> headers, Consumer<String> consumer) {
        // Check if duplicate request is already running
        final String key = generateKey(url);
        mLogger.trace("key=" + key);
        synchronized (mRequestMap) {
            List<Consumer<String>> consumerList;
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

        // Request
        mWebRequest.request(url, headers, new Consumer<String>() {
            @Override
            public void accept(String filePath) {
                List<Consumer<String>> consumerList = null;
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
                        consumerItem.accept(filePath);
                    });
                }
            }
        });
    }

    /**
     * Get cached file
     */
    public String getCachedFile(String url){
        WebData cachedWebData = getCache(url);
        if(cachedWebData!=null){
            String cachedFilePath = cachedWebData.getFilePath();
            boolean result = Files.exists(Paths.get(cachedFilePath));
            if (result == true) {
                return cachedFilePath;
            }
        }
        return null;
    }

    /**
     * Size of cache directory
     */
    public long sizeOfCacheDirectory() {
        String cacheDirectory = FileStore.getInstance().getCacheDirectory();
        long sizeOfCacheDirectory = Util.sizeOfCacheDirectory(cacheDirectory);
        return sizeOfCacheDirectory;
    }

    /**
     * Delete cache file
     */
    public void deleteCache(String url) {
        final String key = generateKey(url);
        FileStore.getInstance().deleteCacheFile(key);
        WebDataStore.getInstance().delete(key);
    }

    /**
     * Get cache file information
     */
    public WebData getCache(String url){
        final String key = generateKey(url);
        return WebDataStore.getInstance().get(key);
    }

    /**
     * Generate key
     */
    public static String generateKey(String url) {
        StringBuffer sb = new StringBuffer();
        String extension = FilenameUtils.getExtension(url);
        sb.append(DigestUtils.md5Hex(url));
        if (StringUtils.isEmpty(extension) == false) {
            sb.append(".");
            sb.append(extension);
        }
        return sb.toString();
    }
}
