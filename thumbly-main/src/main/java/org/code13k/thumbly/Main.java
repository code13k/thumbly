package org.code13k.thumbly;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.code13k.thumbly.app.Env;
import org.code13k.thumbly.app.Status;
import org.code13k.thumbly.business.SecretUrlManager;
import org.code13k.thumbly.config.AppConfig;
import org.code13k.thumbly.config.ChannelConfig;
import org.code13k.thumbly.config.LogConfig;
import org.code13k.thumbly.image.info.CachedImageInfo;
import org.code13k.thumbly.image.processor.CachedImageProcessor;
import org.code13k.thumbly.model.config.app.CacheInfo;
import org.code13k.thumbly.service.api.ApiHttpServer;
import org.code13k.thumbly.service.main.MainHttpServer;
import org.code13k.thumbly.web.client.CachedWebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    /**
     * This is a exceptional code for logging.
     * It depends on LogConfig class.
     * If you modified it, you must modify LogConfig class.
     *
     * @see LogConfig
     */
    static {
        System.setProperty("logback.configurationFile", "config/logback.xml");
    }

    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(Main.class);

    /**
     * Main
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        // Logs
        mLogger.trace("This is TRACE Log!");
        mLogger.debug("This is DEBUG Log!");
        mLogger.info("This is INFO Log!");
        mLogger.warn("This is WARN Log!");
        mLogger.error("This is ERROR Log!");

        // Arguments
        if (args != null) {
            int argsLength = args.length;
            if (argsLength > 0) {
                mLogger.info("------------------------------------------------------------------------");
                mLogger.info("Arguments");
                mLogger.info("------------------------------------------------------------------------");
                for (int i = 0; i < argsLength; i++) {
                    mLogger.info("Args " + i + " = " + args[i]);
                }
                mLogger.info("------------------------------------------------------------------------");

            }
        }

        // System Properties
        mLogger.debug("------------------------------------------------------------------------");
        mLogger.debug("System Property");
        mLogger.debug("------------------------------------------------------------------------");
        System.getProperties().forEach((key, value) -> {
            mLogger.debug(key + " = " + value);
        });
        mLogger.debug("------------------------------------------------------------------------");

        // Initialize
        try {
            LogConfig.getInstance().init();
            AppConfig.getInstance().init();
            ChannelConfig.getInstance().init();
            Env.getInstance().init();
            Status.getInstance().init();
            SecretUrlManager.getInstance().init();
        } catch (Exception e) {
            mLogger.error("Failed to initialize", e);
            System.exit(1);
        }

        // Initialize (CachedWebClient)
        try {
            CacheInfo cacheInfo = AppConfig.getInstance().getCache();
            String cacheDirectory = cacheInfo.getRootDirectory() +"/origin";
            long totalSizeOfOriginImages = AppConfig.getInstance().getCache().getTotalSizeOfOriginImages();
            int eventLoopCount = Math.max(1, Env.getInstance().getProcessorCount() / 2);
            CachedWebClient.getInstance().init(cacheDirectory, totalSizeOfOriginImages, eventLoopCount);
            Thread.sleep(500);
        } catch (Exception e) {
            mLogger.error("Failed to initialize CachedWebClient", e);
            System.exit(2);
        }

        // Initialize (CachedImageInfo)
        try {
            int expirationHours = 24 * 30;
            int operatorCount = Math.max(1, Env.getInstance().getProcessorCount() / 2);
            CachedImageInfo.getInstance().init(".cache/info", expirationHours, operatorCount);
            Thread.sleep(500);
        } catch (Exception e) {
            mLogger.error("Failed to initialize CachedImageInfo", e);
            System.exit(3);
        }

        // Initialize (CachedImageProcessor)
        try {
            CacheInfo cacheInfo = AppConfig.getInstance().getCache();
            String cacheDirectory = cacheInfo.getRootDirectory() +"/thumb";
            long totalSizeOfThumbnailImages = AppConfig.getInstance().getCache().getTotalSizeOfThumbnailImages();
            int operatorCount = Math.max(1, Env.getInstance().getProcessorCount() / 2);
            CachedImageProcessor.getInstance().init(cacheDirectory, totalSizeOfThumbnailImages, operatorCount);
            Thread.sleep(500);
        } catch (Exception e) {
            mLogger.error("Failed to initialize CachedImageProcessor", e);
            System.exit(4);
        }

        // Deploy MainHttpServer
        try {
            DeploymentOptions options = new DeploymentOptions();
            options.setInstances(Math.max(1, Env.getInstance().getProcessorCount() / 2));
            Vertx.vertx().deployVerticle(MainHttpServer.class.getName(), options);
            Thread.sleep(500);
        } catch (Exception e) {
            mLogger.error("Failed to deploy MainHttpServer", e);
            System.exit(5);
        }

        // Deploy APIHttpServer
        try {
            DeploymentOptions options = new DeploymentOptions();
            options.setInstances(Math.max(1, Env.getInstance().getProcessorCount() / 2));
            Vertx.vertx().deployVerticle(ApiHttpServer.class.getName(), options);
            Thread.sleep(500);
        } catch (Exception e) {
            mLogger.error("Failed to deploy ApiHttpServer", e);
            System.exit(6);
        }
    }
}
