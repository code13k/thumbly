package org.code13k.thumbly.service.main;


import org.code13k.thumbly.business.SecretUrlManager;
import org.code13k.thumbly.config.AppConfig;
import org.code13k.thumbly.config.ChannelConfig;
import org.code13k.thumbly.image.processor.CachedImageProcessor;
import org.code13k.thumbly.image.processor.model.Command;
import org.code13k.thumbly.lib.Util;
import org.code13k.thumbly.web.client.CachedWebClient;
import org.code13k.thumbly.model.config.channel.ChannelInfo;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Consumer;


/**
 * Main HTTP Server
 */
public class MainHttpServer extends AbstractVerticle {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(MainHttpServer.class);

    // Const
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static int TCP_SEND_BUFFER_SIZE = 1024 * 1024 * 20; // 20M

    // Server Port
    public static final int PORT = AppConfig.getInstance().getPort().getMainHttp();

    /**
     * start()
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
    }


    /**
     * setStatusRouter()
     */
    private void setRouter(Router router) {
        // /favicon.ico
        router.route("/favicon.ico").handler(routingContext -> {
            sendResponse(routingContext, 404, "Not Found");
        });

        // /:secret_url_path
        router.route("/:secret_url_path").handler(routingContext -> {
            final String secretUrlPathString = routingContext.request().getParam("secret_url_path");
            String requestURL = routingContext.request().uri();

            /**
             * Log
             */
            mLogger.trace("------------------------------------------------------------------------");
            mLogger.trace("Secret URL Request Headers");
            mLogger.trace("------------------------------------------------------------------------");
            mLogger.trace("URL # " + requestURL);
            routingContext.request().headers().forEach(header -> mLogger.trace(header.getKey() + " = " + header.getValue()));
            mLogger.trace("------------------------------------------------------------------------");

            /**
             * Check path
             */
            final String path = SecretUrlManager.getInstance().get(secretUrlPathString);
            mLogger.trace("path=" + path);
            if (StringUtils.isEmpty(path) == true) {
                sendResponse(routingContext, 404, "Not Found");
                return;
            }

            /**
             * Parse
             */
            ArrayList<String> pathItemList = Util.splitString(path, "/");
            if (pathItemList == null || pathItemList.size() < 2) {
                sendResponse(routingContext, 400, "Bad Request. (Invalid URL)");
                return;
            }
            final String commandString = pathItemList.get(0);
            final String channelString = pathItemList.get(1);
            StringBuffer sb = new StringBuffer();
            for (int i = 2; i < pathItemList.size(); i++) {
                sb.append(pathItemList.get(i));
                if (i < (pathItemList.size() - 1)) {
                    sb.append("/");
                }
            }
            final String pathString = sb.toString();


            /**
             * Handler
             */
            commonHandler(routingContext, commandString, channelString, pathString, true);
        });

        // /:command/:channel/:path
        router.route("/:command/:channel/*").handler(routingContext -> {
            final String requestUrl = routingContext.request().uri();
            final String commandString = routingContext.request().getParam("command");
            final String channelString = routingContext.request().getParam("channel");
            final String pathString = parsePath(requestUrl, commandString, channelString);

            /**
             * Log
             */
            mLogger.trace("------------------------------------------------------------------------");
            mLogger.trace("Image Request Headers");
            mLogger.trace("------------------------------------------------------------------------");
            mLogger.trace("URL # " + requestUrl);
            routingContext.request().headers().forEach(header -> mLogger.trace(header.getKey() + " = " + header.getValue()));
            mLogger.trace("------------------------------------------------------------------------");

            /**
             * Handler
             */
            commonHandler(routingContext, commandString, channelString, pathString, false);
        });
    }

    private void commonHandler(RoutingContext routingContext, String commandString, String channelString, String pathString, boolean isSecretUrl) {
        long startTime = System.currentTimeMillis();

        /**
         * Check channel
         */
        ChannelInfo channelInfo = ChannelConfig.getInstance().getChannelInfo(channelString);
        if (channelInfo == null) {
            sendResponse(routingContext, 400, "Bad Request. (Invalid Channel)");
            return;
        }
        mLogger.trace("channelInfo = " + channelInfo);
        if (isSecretUrl == true) {
            if (channelInfo.isSecretUrlEnabled() == false) {
                sendResponse(routingContext, 400, "Bad Request. (Invalid URL)");
                return;
            }
        } else {
            if (channelInfo.isNormalUrlEnabled() == false) {
                sendResponse(routingContext, 400, "Bad Request. (Invalid URL)");
                return;
            }
        }


        /**
         * Check command
         */
        final Command command = stringToCommand(commandString);
        if (CachedImageProcessor.isValid(command) == false) {
            sendResponse(routingContext, 400, "Bad Request. (Invalid Command)");
            return;
        }
        mLogger.trace("command = " + command);

        /**
         * Check path
         */
        if (StringUtils.isEmpty(pathString) == true) {
            sendResponse(routingContext, 400, "Bad Request. (Invalid Path)");
            return;
        }
        mLogger.trace("pathString = " + pathString);

        /**
         * Init origin url
         */
        String originUrl = channelInfo.getBaseUrl() + "/" + pathString;
        mLogger.debug("originUrl = " + originUrl);

        /**
         * Get origin file
         */
        CachedWebClient.getInstance().getFile(originUrl, null, new Consumer<String>() {
            @Override
            public void accept(String originFilePath) {
                mLogger.debug("originFilePath = " + originFilePath);

                // Processing Time (1)
                mLogger.debug("processing time #1 : " + Util.processingTime(startTime) + "ms");

                // Check origin file
                if (StringUtils.isEmpty(originFilePath) == true) {
                    sendResponse(routingContext, 404, "Not Found");
                    return;
                }

                // Get origin file
                if (command.getType() == Command.Type.ORIGIN) {
                    sendFile(routingContext, originFilePath, channelInfo.getBrowserCacheExpiration(), isSecretUrl, new Consumer<Void>() {
                        @Override
                        public void accept(Void aVoid) {
                            // Processing Time (2)
                            mLogger.debug("processing time #2 : " + Util.processingTime(startTime) + "ms");
                        }
                    });
                    return;
                }

                /**
                 * Get thumbnail file
                 */
                CachedImageProcessor.getInstance().process(originFilePath, command, new Consumer<String>() {
                    @Override
                    public void accept(String thumbFilePath) {
                        mLogger.debug("thumbFilePath = " + thumbFilePath);
                        sendFile(routingContext, thumbFilePath, channelInfo.getBrowserCacheExpiration(), isSecretUrl, new Consumer<Void>() {
                            @Override
                            public void accept(Void aVoid) {
                                // Processing Time (4)
                                mLogger.debug("processing time #4 : " + Util.processingTime(startTime) + "ms");
                            }
                        });

                        // Processing Time (3)
                        mLogger.debug("processing time #3 : " + Util.processingTime(startTime) + "ms");
                        return;
                    }
                });

            }
        });
    }

    /**
     * sendResponse()
     */
    private void sendResponse(RoutingContext routingContext, int statusCode, String statusMessage) {
        routingContext.response().putHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        routingContext.response().setStatusCode(statusCode).setStatusMessage(statusMessage).end(statusMessage);
    }

    /**
     * sendFile()
     */
    private void sendFile(RoutingContext routingContext,
                          String filePath,
                          int browserCacheExpiration,
                          boolean isSecretUrl,
                          Consumer<Void> consumer) {

        /**
         * Secret URL
         */
        if (isSecretUrl == true) {
            // Cache-Control
            routingContext.response().putHeader(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            routingContext.response().putHeader(HttpHeaderNames.PRAGMA, "no-cache");
            routingContext.response().putHeader(HttpHeaderNames.EXPIRES, "0");

            // Content-Type
            FileNameMap mimeTypes = URLConnection.getFileNameMap();
            String contentType = mimeTypes.getContentTypeFor(filePath);
            if (StringUtils.isEmpty(contentType) == false) {
                routingContext.response().putHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
            }

            // 200 (OK)
            routingContext.response().putHeader(HttpHeaderNames.ACCEPT_RANGES, "bytes");
            routingContext.response().putHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            routingContext.response().sendFile(filePath);
        }


        /**
         * Normal URL
         */
        else {
            // Init
            File file = new File(filePath);
            int maxAge = browserCacheExpiration;
            long lastModifiedTimeMillis = file.lastModified();

            // Check to modify
            boolean isModified = false;
            final String headerIfModifiedSince = routingContext.request().headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
            if (StringUtils.isEmpty(headerIfModifiedSince)) {
                isModified = true;
            } else {
                try {
                    SimpleDateFormat format = new SimpleDateFormat(HTTP_DATE_FORMAT);
                    Date date = format.parse(headerIfModifiedSince);
                    long ifModifiedSince = date.getTime();
                    mLogger.trace("ifModifiedSince : " + ifModifiedSince + ", lastModified : " + lastModifiedTimeMillis);
                    isModified = (lastModifiedTimeMillis > ifModifiedSince);
                } catch (Exception e) {
                    isModified = true;
                }
            }

            // Cache-Control
            routingContext.response().putHeader(HttpHeaderNames.CACHE_CONTROL, "public, max-age=" + maxAge);

            // Expires
            SimpleDateFormat expiresFormat = new SimpleDateFormat(HTTP_DATE_FORMAT);
            expiresFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            String expiresString = expiresFormat.format(lastModifiedTimeMillis);
            routingContext.response().putHeader(HttpHeaderNames.EXPIRES, expiresString);

            // Last-Modified
            if (isModified == true) {
                SimpleDateFormat lastModifiedFormat = new SimpleDateFormat(HTTP_DATE_FORMAT);
                lastModifiedFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String lastModifiedString = lastModifiedFormat.format(lastModifiedTimeMillis);
                routingContext.response().putHeader(HttpHeaderNames.LAST_MODIFIED, lastModifiedString);
            }

            // Content-Type
            FileNameMap mimeTypes = URLConnection.getFileNameMap();
            String contentType = mimeTypes.getContentTypeFor(filePath);
            if (StringUtils.isEmpty(contentType) == false) {
                routingContext.response().putHeader(HttpHeaderNames.CONTENT_TYPE, contentType);
            }

            // Date
            SimpleDateFormat currentDateFormat = new SimpleDateFormat(HTTP_DATE_FORMAT);
            currentDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            String currentDateString = currentDateFormat.format(System.currentTimeMillis());
            routingContext.response().putHeader(HttpHeaderNames.DATE, currentDateString);

            // Not Modified (304)
            if (isModified == false) {
                String statusMessage = "Not Modified";
                routingContext.response().setStatusCode(304).setStatusMessage(statusMessage).end(statusMessage);
            }

            // OK (200)
            else {
                routingContext.response().putHeader(HttpHeaderNames.ACCEPT_RANGES, "bytes");
                routingContext.response().putHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                routingContext.response().sendFile(filePath);
            }
        }

        /**
         * End
         */
        if (mLogger.isTraceEnabled()) {
            mLogger.trace("------------------------------------------------------------------------");
            mLogger.trace("Send File");
            mLogger.trace("------------------------------------------------------------------------");
            mLogger.trace("FILE # " + filePath);
            routingContext.response().headers().forEach(header -> mLogger.trace(header.getKey() + " = " + header.getValue()));
            mLogger.trace("------------------------------------------------------------------------");
        }
        consumer.accept(null);
    }

    /**
     * Parse path
     */
    private String parsePath(String requestUrl, String command, String channel) {
        if (StringUtils.isEmpty(requestUrl) == true) {
            return null;
        }
        if (StringUtils.isEmpty(command) == true) {
            return null;
        }
        if (StringUtils.isEmpty(channel) == true) {
            return null;
        }
        StringBuffer pathPrefixBuffer = new StringBuffer();
        pathPrefixBuffer.append("/");
        pathPrefixBuffer.append(command);
        pathPrefixBuffer.append("/");
        pathPrefixBuffer.append(channel);
        pathPrefixBuffer.append("/");
        String pathPrefix = pathPrefixBuffer.toString();
        int index = StringUtils.indexOf(requestUrl, pathPrefix);
        String path = StringUtils.substring(requestUrl, index + pathPrefix.length());
        return path;
    }

    /**
     * Convert string to Command object
     * <p>
     * [ Sample ]
     * thumb-200x0-webp-100
     * resize-200x200-origin
     * origin
     * thumb-0x200
     * resize-100x100-origin-50
     * origin-origin-origin-50
     * <p>
     * [ Command Syntax ]
     * type-size-format-quality
     */
    public static Command stringToCommand(String command) {
        Command result = new Command();

        // Parse
        ArrayList<String> commandList = Util.splitString(command, "-");

        // Set type
        try {
            String type = commandList.get(0);
            result.setType(type);
        } catch (Exception e) {
            return null;
        }

        // Set size
        try {
            String size = commandList.get(1);
            result.setSize(size);
        } catch (Exception e) {
            result.setSize(0, 0);
        }

        // Set format
        try {
            String format = commandList.get(2);
            result.setFormat(format);
        } catch (Exception e) {
            result.setFormat(Command.Format.ORIGIN);
        }

        // Set quality
        try {
            String quality = commandList.get(3);
            result.setQuality(quality);
        } catch (Exception e) {
            result.setQuality(100);
        }

        return result;
    }
}