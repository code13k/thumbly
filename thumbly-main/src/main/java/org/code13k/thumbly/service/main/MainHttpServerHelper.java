package org.code13k.thumbly.service.main;

import com.google.gson.GsonBuilder;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.image.processor.model.Command;
import org.code13k.thumbly.lib.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainHttpServerHelper {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(MainHttpServer.class);

    // Const
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    /**
     * Send response
     */
    public static void sendResponse(RoutingContext routingContext, int statusCode, String statusMessage) {
        routingContext.response().putHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        routingContext.response().setStatusCode(statusCode).setStatusMessage(statusMessage).end(statusMessage);
    }

    /**
     * Send status
     */
    public static void sendStatus(RoutingContext routingContext, String statusMessage) {
        Map<String, Object> jsonResult = new HashMap<>();
        jsonResult.put("status", statusMessage);
        String result = new GsonBuilder().create().toJson(jsonResult);

        HttpServerResponse response = routingContext.response();
        response.putHeader(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        response.putHeader(HttpHeaderNames.PRAGMA, "no-cache");
        response.putHeader(HttpHeaderNames.EXPIRES, "0");
        response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        response.putHeader(HttpHeaderNames.ACCEPT_RANGES, "bytes");
        response.putHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.end(result);
        response.close();
    }

    /**
     * Send file
     */
    public static void sendFile(RoutingContext routingContext, String filePath, int browserCacheExpiration, boolean isSecretUrl) {

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
    }

    /**
     * Parse secret path
     */
    public static ArrayList<String> parseSecretPath(String path) {
        ArrayList<String> pathItemList = Util.splitString(path, "/");
        if (pathItemList == null || pathItemList.size() < 2) {
            return null;
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
        final ArrayList<String> result = new ArrayList<>();
        result.add(commandString);
        result.add(channelString);
        result.add(pathString);
        return result;
    }

    /**
     * Parse normal path
     */
    public static String parseNormalPath(String requestUrl, String command, String channel) {
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
