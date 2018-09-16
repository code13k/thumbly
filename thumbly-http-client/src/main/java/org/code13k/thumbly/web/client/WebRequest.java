package org.code13k.thumbly.web.client;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.code13k.thumbly.web.client.aws.AwsS3SignValue;
import org.code13k.thumbly.web.client.aws.AwsS3SignerV4;
import org.code13k.thumbly.web.client.model.WebData;
import org.code13k.thumbly.web.client.model.ResponseHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.function.Consumer;

public class WebRequest {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(WebRequest.class);

    // Data
    private WebClient mWebClient;

    /**
     * Constructor
     */
    public WebRequest(int eventLoopPoolSize, String userAgent) {
        mLogger.trace("WebRequest()");

        // Set User Agent
        if (StringUtils.isEmpty(userAgent) == true) {
            userAgent = "Code13k-Thumbly";
        }
        mLogger.trace("userAgent = " + userAgent);

        // Set Web Client
        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions.setUserAgent(userAgent);
        webClientOptions.setTrustAll(true);
        webClientOptions.setSsl(true);
        webClientOptions.setTryUseCompression(true);
        webClientOptions.setConnectTimeout(1000 * 10); // Seconds
        webClientOptions.setIdleTimeout(1000 * 20); // Seconds
        VertxOptions vertxOptions = new VertxOptions();
        vertxOptions.setEventLoopPoolSize(eventLoopPoolSize);
        mWebClient = WebClient.create(Vertx.vertx(vertxOptions), webClientOptions);
    }

    /**
     * Request
     */
    public void request(String url, AwsS3SignValue awsS3SignValue, Consumer<String> consumer) {
        final long startTime = System.currentTimeMillis();
        final String urlKey = CachedWebClient.generateKey(url);
        WebData tempWebData = WebDataStore.getInstance().get(urlKey);

        /**
         * Cache HIT
         */
        if (tempWebData != null) {
            long currentTimeSeconds = Util.getCurrentTimeSeconds();
            if (tempWebData.getExpiredTimeSeconds() > currentTimeSeconds) {
                boolean isFileExists = Files.exists(Paths.get(tempWebData.getFilePath()));
                if (isFileExists == true) {
                    mLogger.debug("Cache HIT!!");
                    consumer.accept(tempWebData.getFilePath());
                    return;
                } else {
                    mLogger.error("Cache hit but file not found");
                    WebDataStore.getInstance().delete(urlKey);
                    tempWebData = null;
                }
            }
        }
        final WebData webData = tempWebData;


        /**
         * Not Cache HIT
         */
        HttpRequest<Buffer> httpRequest = mWebClient.getAbs(url);

        // Set Headers (Cache)
        if (webData == null) {
            httpRequest.putHeader("Cache-Control", "no-cache");
            httpRequest.putHeader("Pragma", "no-cache");
        } else {
            ResponseHeaders tempResponseHeaders = webData.getResponseHeaders();
            httpRequest.putHeader("Cache-Control", "max-age=0");
            if (StringUtils.isEmpty(tempResponseHeaders.getLastModified()) == false) {
                httpRequest.putHeader("If-Modified-Since", tempResponseHeaders.getLastModified());
            }
            if (StringUtils.isEmpty(tempResponseHeaders.getEtag()) == false) {
                httpRequest.putHeader("If-None-Match", tempResponseHeaders.getEtag());
            }
        }

        // AWS S3 Signer
        if (awsS3SignValue != null) {
            AwsS3SignerV4.putHeaders(httpRequest.headers(), url, awsS3SignValue);
        }

        // Log
        mLogger.trace("------------------------------------------------------------------------");
        mLogger.trace("Web Request Headers");
        mLogger.trace("------------------------------------------------------------------------");
        mLogger.trace("URL # " + url);
        httpRequest.headers().forEach(header -> mLogger.trace(header.getKey() + " = " + header.getValue()));
        mLogger.trace("------------------------------------------------------------------------");

        // Request
        httpRequest.send(requestResult -> {
            if (requestResult.succeeded()) {
                final HttpResponse response = requestResult.result();

                // Log
                mLogger.trace("------------------------------------------------------------------------");
                mLogger.trace("Web Response Headers");
                mLogger.trace("------------------------------------------------------------------------");
                mLogger.trace("URL # " + url);
                mLogger.trace("STATUS # " + response.statusCode() + " " + response.statusMessage());
                response.headers().forEach(header -> mLogger.trace(header.getKey() + " = " + header.getValue()));
                mLogger.trace("------------------------------------------------------------------------");

                // Get Headers
                ResponseHeaders responseHeaders = new ResponseHeaders();
                responseHeaders.fromMultiMap(response.headers());

                // Status Code : 5xx
                if (response.statusCode() >= 500 && response.statusCode() <= 599) {
                    consumer.accept(null);

                    // Log
                    String errorMessage = "getFile() Status Code : " + response.statusCode() + " # " + url + " : " + requestResult.cause();
                    mLogger.error(errorMessage);
                }

                // Status Code : 4xx
                else if (response.statusCode() >= 400 && response.statusCode() <= 499) {
                    consumer.accept(null);
                    WebDataStore.getInstance().delete(urlKey);

                    // Log
                    String errorMessage = "getFile() Status Code : " + response.statusCode() + " # " + url + " : " + requestResult.cause();
                    mLogger.error(errorMessage);
                }

                // Status Code : 304
                else if (response.statusCode() == 304) {
                    // Update Cached Headers
                    ResponseHeaders tempResponseHeaders = webData.getResponseHeaders();
                    if (StringUtils.isEmpty(tempResponseHeaders.getCacheControl()) == false) {
                        responseHeaders.setCacheControl(tempResponseHeaders.getCacheControl());
                    }
                    if (StringUtils.isEmpty(tempResponseHeaders.getExpires()) == false) {
                        responseHeaders.setExpires(tempResponseHeaders.getExpires());
                    }
                    if (StringUtils.isEmpty(tempResponseHeaders.getLastModified()) == false) {
                        responseHeaders.setLastModified(tempResponseHeaders.getLastModified());
                    }

                    // Calculate Expires Time
                    long cachingTimeSeconds = calculateCachingTime(responseHeaders);
                    long currentTimeSeconds = Util.getCurrentTimeSeconds();
                    long expiredTimeSeconds = cachingTimeSeconds + currentTimeSeconds;

                    // Save to cache store
                    webData.setExpiredTimeSeconds(expiredTimeSeconds);
                    boolean result = WebDataStore.getInstance().set(urlKey, webData);
                    if (result == false) {
                        String errorMessage = "getFile() failed to cache (304) # " + url;
                        mLogger.error(errorMessage);
                    }
                    consumer.accept(webData.getFilePath());
                }

                // Status Code : 200
                else if (response.statusCode() == 200) {
                    // Processing Time (1)
                    mLogger.debug("processing time (200) #1 : " + Util.processingTime(startTime) + "ms");

                    // Get Body (Temp)
                    final String tempFilePath = FileStore.getInstance().saveToTempFile(response.bodyAsBuffer().getBytes());

                    // Processing Time (2)
                    mLogger.debug("processing time (200) #2 : " + Util.processingTime(startTime) + "ms");

                    // Get Body (Cached)
                    final String cachedFilePath = FileStore.getInstance().moveToCacheFile(tempFilePath, urlKey);

                    // Processing Time (3)
                    mLogger.debug("processing time (200) #3 : " + Util.processingTime(startTime) + "ms");

                    // Calculate Expires Time
                    long cachingTimeSeconds = calculateCachingTime(responseHeaders);
                    long currentTimeSeconds = Util.getCurrentTimeSeconds();
                    long expiredTimeSeconds = cachingTimeSeconds + currentTimeSeconds;

                    // Save to cache store
                    WebData newWebData = new WebData();
                    newWebData.setUrl(url);
                    newWebData.setFilePath(cachedFilePath);
                    newWebData.setExpiredTimeSeconds(expiredTimeSeconds);
                    newWebData.setResponseHeaders(responseHeaders);
                    boolean result = WebDataStore.getInstance().set(urlKey, newWebData);
                    if (result == false) {
                        String errorMessage = "getFile() failed to cache (200) # " + url;
                        mLogger.error(errorMessage);
                    }
                    consumer.accept(cachedFilePath);

                    // Processing Time (4)
                    mLogger.debug("processing time (200) #4 : " + Util.processingTime(startTime) + "ms");
                }

                // Status Code : Unexpected
                else {
                    consumer.accept(null);
                    // Log
                    String errorMessage = "getFile() status code is " + response.statusCode() + " # " + url + " : " + requestResult.cause();
                    mLogger.error(errorMessage);
                    return;
                }
            }

            // Request Failed
            else {
                String errorMessage = "getFile() failed # " + url + " : " + requestResult.cause();
                mLogger.error(errorMessage);
                consumer.accept(null);
            }
        });
    }

    /**
     * Calculate Caching Time (Seconds)
     * <p>
     * 1. "max-age" of "cache-control"
     * 2. time difference between "expires" and "date"
     * 3. 10% of time difference between "last-modified" and current time
     * 4. 24 hours
     * <p>
     * https://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
     */
    private long calculateCachingTime(ResponseHeaders responseHeaders) {
        // (1) "max-age" of "cache-control"
        String maxAgeString = responseHeaders.getMaxAgeOfCacheControl();
        if (StringUtils.isEmpty(maxAgeString) == false) {
            long expiresTimeSeconds = NumberUtils.toLong(maxAgeString, 0L);
            mLogger.debug("(1) expires seconds : " + expiresTimeSeconds);
            return expiresTimeSeconds;
        }

        // (2) time difference between "expires" and "date"
        Date date = responseHeaders.getDateAsDate();
        Date expires = responseHeaders.getExpiresAsDate();
        if (date != null && expires != null) {
            long expiresTimeMillis = expires.getTime() - date.getTime();
            if (expiresTimeMillis >= 0) {
                long expiresTimeSeconds = expiresTimeMillis / 1000;
                mLogger.debug("(2) expires seconds : " + expiresTimeSeconds);
                return expiresTimeSeconds;
            }
        }

        // (3) 10% of time difference between "last-modified" and current time
        Date currentDate = new Date();
        Date lastModified = responseHeaders.getLastModifiedAsDate();
        if (currentDate != null && lastModified != null) {
            long expiresMillis = currentDate.getTime() - lastModified.getTime();
            if (expiresMillis >= 0) {
                expiresMillis = (int) (expiresMillis * 0.1);
                long expiresSeconds = expiresMillis / 1000;
                mLogger.debug("(3) expires seconds : " + expiresSeconds);
                return expiresSeconds;
            }
        }

        // (4) 24 hours
        long expiresSeconds = 60 * 60 * 24;
        mLogger.debug("(4) expires seconds : " + expiresSeconds);
        return expiresSeconds;
    }
}
