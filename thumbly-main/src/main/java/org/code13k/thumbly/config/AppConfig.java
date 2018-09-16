package org.code13k.thumbly.config;


import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.lib.Util;
import org.code13k.thumbly.model.config.app.CacheInfo;
import org.code13k.thumbly.model.config.app.PortInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.LinkedHashMap;

public class AppConfig extends BasicConfig {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(AppConfig.class);

    // Data
    private PortInfo mPortInfo = new PortInfo();
    private CacheInfo mCacheInfo = new CacheInfo();

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final AppConfig INSTANCE = new AppConfig();
    }

    public static AppConfig getInstance() {
        return AppConfig.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private AppConfig() {
        mLogger.trace("AppConfig()");
    }

    @Override
    protected String getDefaultConfigFilename() {
        return "default_app_config.yml";
    }

    @Override
    protected String getConfigFilename() {
        return "app_config.yaml";
    }

    @Override
    protected boolean loadConfig(final String content, final String filePath) {
        try {
            Yaml yaml = new Yaml();
            LinkedHashMap yamlObject = yaml.load(content);
            mLogger.trace("yamlObject class name = " + yamlObject.getClass().getName());
            mLogger.trace("yamlObject = " + yamlObject);

            // PortInfo
            LinkedHashMap portObject = (LinkedHashMap) yamlObject.get("port");
            mLogger.trace("portObject class name = " + portObject.getClass().getName());
            mLogger.trace("portObject = " + portObject);
            Integer portMainHttp = (Integer) portObject.get("main_http");
            if (Util.isValidPortNumber(portMainHttp) == false) {
                mLogger.error("Invalid main_http of port : " + portMainHttp);
                return false;
            }
            Integer portApiHttp = (Integer) portObject.get("api_http");
            if (Util.isValidPortNumber(portApiHttp) == false) {
                mLogger.error("Invalid api_http of port : " + portApiHttp);
                return false;
            }
            if (portMainHttp == portApiHttp) {
                mLogger.error("Duplicated port number : main_http=" + portMainHttp + ", api_http=" + portApiHttp);
                return false;
            }
            mPortInfo.setMainHttp(portMainHttp);
            mPortInfo.setApiHttp(portApiHttp);

            // CacheInfo
            String temp;
            LinkedHashMap cacheObject = (LinkedHashMap) yamlObject.get("cache");
            String cacheRootDirectory = (String) cacheObject.get("root_directory");
            if (StringUtils.isEmpty(cacheRootDirectory) == true) {
                mLogger.error("Invalid root_directory or cache : " + cacheRootDirectory);
                return false;
            }
            temp = (String) cacheObject.get("total_size_of_origin_images");
            long totalSizeOfOriginImages = Util.toBytes(temp);
            if (totalSizeOfOriginImages <= 0L) {
                mLogger.error("Invalid total_size_of_origin_images of cache");
                return false;
            }
            temp = (String) cacheObject.get("total_size_of_thumbnail_images");
            long totalSizeOfThumbnailImages = Util.toBytes(temp);
            if (totalSizeOfThumbnailImages <= 0L) {
                mLogger.error("Invalid total_size_of_thumbnail_images of cache");
                return false;
            }
            mCacheInfo.setRootDirectory(cacheRootDirectory);
            mCacheInfo.setTotalSizeOfOriginImages(totalSizeOfOriginImages);
            mCacheInfo.setTotalSizeOfThumbnailImages(totalSizeOfThumbnailImages);
        } catch (Exception e) {
            mLogger.error("Failed to load config file", e);
            return false;
        }
        return true;
    }

    @Override
    public void logging() {
        // Begin
        mLogger.info("------------------------------------------------------------------------");
        mLogger.info("Application Configuration");
        mLogger.info("------------------------------------------------------------------------");

        // Config File Path
        mLogger.info("Config file path = " + getConfigFilename());

        // PortInfo
        mLogger.info("main_http of PortInfo = " + mPortInfo.getMainHttp());
        mLogger.info("api_http of PortInfo = " + mPortInfo.getApiHttp());

        // ClusterInfo
        mLogger.info("root_directory of CacheInfo = " + mCacheInfo.getRootDirectory());
        mLogger.info("total_size_of_origin_images of CacheInfo = " + mCacheInfo.getTotalSizeOfOriginImages());
        mLogger.info("total_size_of_thumbnail_images of CacheInfo = " + mCacheInfo.getTotalSizeOfThumbnailImages());

        // End
        mLogger.info("------------------------------------------------------------------------");
    }

    /**
     * Get port
     */
    public PortInfo getPort() {
        return mPortInfo;
    }

    /**
     * Get cache
     */
    public CacheInfo getCache() {
        return mCacheInfo;
    }
}
