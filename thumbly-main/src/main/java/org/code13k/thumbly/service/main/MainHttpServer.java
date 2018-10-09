package org.code13k.thumbly.service.main;


import io.vertx.core.http.HttpMethod;
import org.code13k.thumbly.business.ClusteredSecretUrl;
import org.code13k.thumbly.config.AppConfig;
import org.code13k.thumbly.config.ChannelConfig;
import org.code13k.thumbly.image.processor.CachedImageProcessor;
import org.code13k.thumbly.image.processor.model.Command;
import org.code13k.thumbly.lib.Util;
import org.code13k.thumbly.model.config.channel.AwsS3Info;
import org.code13k.thumbly.web.client.CachedWebClient;
import org.code13k.thumbly.model.config.channel.ChannelInfo;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.web.client.aws.AwsS3SignValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.Consumer;


/**
 * Main HTTP Server
 */
public class MainHttpServer extends AbstractVerticle {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(MainHttpServer.class);

    // Const
    private static int TCP_SEND_BUFFER_SIZE = 1024 * 1024 * 20; // 20M

    // Server Port
    public static final int PORT = AppConfig.getInstance().getPort().getMainHttp();

    /**
     * Start
     */
    @Override
    public void start() throws Exception {
        super.start();
        mLogger.info("start()");

        // Init HTTP Server
        HttpServerOptions httpServerOptions = new HttpServerOptions();
        httpServerOptions.setCompressionSupported(true);
        httpServerOptions.setPort(PORT);
        httpServerOptions.setSendBufferSize(TCP_SEND_BUFFER_SIZE);
        httpServerOptions.setTcpKeepAlive(true);
        httpServerOptions.setTcpNoDelay(false); // Use Nagle's Algorithm
        HttpServer httpServer = vertx.createHttpServer(httpServerOptions);

        // Routing
        Router router = Router.router(vertx);
        setRouter(router);

        // Listen
        httpServer.requestHandler(router::accept).listen();

        // End
        logging(httpServerOptions, router);
    }

    /**
     * Logging
     */
    private void logging(HttpServerOptions httpServerOptions, Router router) {
        synchronized (mLogger) {
            // Begin
            mLogger.info("------------------------------------------------------------------------");
            mLogger.info("Main HTTP Server");
            mLogger.info("------------------------------------------------------------------------");

            // Vert.x
            mLogger.info("Vert.x clustered = " + getVertx().isClustered());
            mLogger.info("Vert.x deployment ID = " + deploymentID());

            // Http Server Options
            mLogger.info("Port = " + httpServerOptions.getPort());
            mLogger.info("Idle timeout (second) = " + httpServerOptions.getIdleTimeout());
            mLogger.info("Compression supported = " + httpServerOptions.isCompressionSupported());
            mLogger.info("Compression level = " + httpServerOptions.getCompressionLevel());

            // Route
            router.getRoutes().forEach(r -> {
                mLogger.info("Routing path = " + r.getPath());
            });

            // End
            mLogger.info("------------------------------------------------------------------------");
        }
    }

    /**
     * Set router
     */
    private void setRouter(Router router) {
        // /favicon.ico
        router.route("/favicon.ico").handler(routingContext -> {
            MainHttpServerHelper.sendResponse(routingContext, 404, "Not Found");
        });

        // /status/:secret_path
        router.route().method(HttpMethod.GET).path("/status/:secret_path").handler(routingContext -> {
            routeSecretUrl(routingContext, true);
        });

        // /status/:command/:channel/:path
        router.route().method(HttpMethod.GET).path("/status/:command/:channel/*").handler(routingContext -> {
            routeNormalUrl(routingContext, true);
        });

        // /:secret_path
        router.route().method(HttpMethod.GET).path("/:secret_path").handler(routingContext -> {
            routeSecretUrl(routingContext, false);
        });

        // /:command/:channel/:path
        router.route().method(HttpMethod.GET).path("/:command/:channel/*").handler(routingContext -> {
            routeNormalUrl(routingContext, false);
        });
    }

    /**
     * Route secret url
     */
    private void routeSecretUrl(RoutingContext routingContext, boolean isStatus) {
        final String secretPathString = routingContext.request().getParam("secret_path");
        final String requestURL = routingContext.request().uri();

        /**
         * Log
         */
        mLogger.trace("------------------------------------------------------------------------");
        mLogger.trace("Secret Request Headers");
        mLogger.trace("------------------------------------------------------------------------");
        mLogger.trace("URL # " + requestURL);
        routingContext.request().headers().forEach(header -> mLogger.trace(header.getKey() + " = " + header.getValue()));
        mLogger.trace("------------------------------------------------------------------------");

        /**
         * Check secret path
         */
        ClusteredSecretUrl.getInstance().get(secretPathString, new Consumer<String>() {
            @Override
            public void accept(String path) {
                mLogger.trace("path=" + path);
                if (StringUtils.isEmpty(path) == true) {
                    MainHttpServerHelper.sendResponse(routingContext, 404, "Not Found");
                    return;
                }

                /**
                 * Parse
                 */
                final ArrayList<String> parsedPath = MainHttpServerHelper.parseSecretPath(path);
                if (parsedPath == null) {
                    MainHttpServerHelper.sendResponse(routingContext, 400, "Bad Request. (Invalid URL)");
                    return;
                }
                final String commandString = parsedPath.get(0);
                final String channelString = parsedPath.get(1);
                final String pathString = parsedPath.get(2);

                /**
                 * Handler
                 */
                handler(routingContext, commandString, channelString, pathString, true, isStatus);
            }
        });
    }

    /**
     * Route normal url
     */
    private void routeNormalUrl(RoutingContext routingContext, boolean isStatus) {
        final String requestUrl = routingContext.request().uri();
        final String commandString = routingContext.request().getParam("command");
        final String channelString = routingContext.request().getParam("channel");
        final String pathString = MainHttpServerHelper.parseNormalPath(requestUrl, commandString, channelString);

        /**
         * Log
         */
        mLogger.trace("------------------------------------------------------------------------");
        mLogger.trace("Normal Request Headers");
        mLogger.trace("------------------------------------------------------------------------");
        mLogger.trace("URL # " + requestUrl);
        routingContext.request().headers().forEach(header -> mLogger.trace(header.getKey() + " = " + header.getValue()));
        mLogger.trace("------------------------------------------------------------------------");

        /**
         * Handler
         */
        handler(routingContext, commandString, channelString, pathString, false, isStatus);
    }

    /**
     * Handler
     */
    private void handler(RoutingContext routingContext,
                         String commandString,
                         String channelString,
                         String pathString,
                         boolean isSecretUrl,
                         boolean isStatus) {
        long startTime = System.currentTimeMillis();

        /**
         * Log
         */
        if (mLogger.isTraceEnabled()) {
            mLogger.trace("commandString = " + commandString);
            mLogger.trace("channelString = " + channelString);
            mLogger.trace("pathString = " + pathString);
            mLogger.trace("isSecretUrl = " + isSecretUrl);
            mLogger.trace("isStatus = " + isStatus);
        }

        /**
         * Check channel
         */
        ChannelInfo channelInfo = ChannelConfig.getInstance().getChannelInfo(channelString);
        if (channelInfo == null) {
            MainHttpServerHelper.sendResponse(routingContext, 400, "Bad Request. (Invalid Channel)");
            return;
        }
        mLogger.trace("channelInfo = " + channelInfo);
        if (isSecretUrl == true) {
            if (channelInfo.isSecretUrlEnabled() == false) {
                MainHttpServerHelper.sendResponse(routingContext, 403, "Forbidden. (Not supported secret url)");
                return;
            }
        } else {
            if (channelInfo.isNormalUrlEnabled() == false) {
                MainHttpServerHelper.sendResponse(routingContext, 403, "Forbidden. (Not supported normal url)");
                return;
            }
        }

        /**
         * Check command
         */
        final Command command = MainHttpServerHelper.stringToCommand(commandString);
        if (CachedImageProcessor.isValid(command) == false) {
            MainHttpServerHelper.sendResponse(routingContext, 400, "Bad Request. (Invalid Command)");
            return;
        }
        mLogger.trace("command = " + command);

        /**
         * Check path
         */
        if (StringUtils.isEmpty(pathString) == true) {
            MainHttpServerHelper.sendResponse(routingContext, 400, "Bad Request. (Invalid Path)");
            return;
        }
        mLogger.trace("pathString = " + pathString);

        /**
         * Init origin url
         */
        String originUrl = channelInfo.getBaseUrl() + "/" + pathString;
        mLogger.debug("originUrl = " + originUrl);

        /**
         * Get status
         */
        if (isStatus == true) {
            /**
             * Get origin file
             */
            String cachedOriginFilePath = CachedWebClient.getInstance().getCachedFile(originUrl);
            mLogger.debug("cachedOriginFilePath = " + cachedOriginFilePath);

            // Check origin file
            if (StringUtils.isEmpty(cachedOriginFilePath) == true) {
                MainHttpServerHelper.sendStatus(routingContext, false);
                return;
            }

            // Get origin file
            if (command.getType() == Command.Type.ORIGIN) {
                MainHttpServerHelper.sendStatus(routingContext, true);
                return;
            }

            /**
             * Get thumbnail file
             */
            String cachedThumbFilePath = CachedImageProcessor.getInstance().getCachedFile(cachedOriginFilePath, command);
            mLogger.debug("cachedThumbFilePath = " + cachedThumbFilePath);
            if (StringUtils.isEmpty(cachedThumbFilePath) == true) {
                MainHttpServerHelper.sendStatus(routingContext, false);
                return;
            } else {
                MainHttpServerHelper.sendStatus(routingContext, true);
                return;
            }
        }

        /**
         * Get image
         */
        else {
            /**
             * AWS S3
             */
            AwsS3SignValue awsS3SignValue = null;
            if (channelInfo.getType() == ChannelConfig.ChannelType.AWS_S3) {
                AwsS3Info awsS3Info = (AwsS3Info) channelInfo;
                if (StringUtils.isEmpty(awsS3Info.getAccessKey()) == false) {
                    if (StringUtils.isEmpty(awsS3Info.getSecretKey()) == false) {
                        awsS3SignValue = new AwsS3SignValue();
                        awsS3SignValue.setAccessKey(awsS3Info.getAccessKey());
                        awsS3SignValue.setSecretKey(awsS3Info.getSecretKey());
                        awsS3SignValue.setRegion(awsS3Info.getRegion());
                    }
                }
            }
            mLogger.trace("awsS3SignValue = " + awsS3SignValue);

            /**
             * Get origin file
             */
            CachedWebClient.getInstance().getFile(originUrl, awsS3SignValue, new Consumer<String>() {
                @Override
                public void accept(String originFilePath) {
                    mLogger.debug("originFilePath = " + originFilePath);

                    // Processing Time (1)
                    mLogger.debug("processing time #1 : " + Util.processingTime(startTime) + "ms");

                    // Check origin file
                    if (StringUtils.isEmpty(originFilePath) == true) {
                        MainHttpServerHelper.sendResponse(routingContext, 404, "Not Found");
                        return;
                    }

                    // Get origin file
                    if (command.getType() == Command.Type.ORIGIN) {
                        int expiration = channelInfo.getBrowserCacheExpiration();
                        MainHttpServerHelper.sendFile(routingContext, originFilePath, expiration, isSecretUrl);
                        return;
                    }

                    /**
                     * Get thumbnail file
                     */
                    CachedImageProcessor.getInstance().process(originFilePath, command, new Consumer<String>() {
                        @Override
                        public void accept(String thumbFilePath) {
                            mLogger.debug("thumbFilePath = " + thumbFilePath);
                            int expiration = channelInfo.getBrowserCacheExpiration();
                            MainHttpServerHelper.sendFile(routingContext, thumbFilePath, expiration, isSecretUrl);

                            // Processing Time (2)
                            mLogger.debug("processing time #2 : " + Util.processingTime(startTime) + "ms");
                            return;
                        }
                    });

                }
            });
        }
    }
}