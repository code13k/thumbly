package org.code13k.thumbly.service.api;

import org.code13k.thumbly.config.AppConfig;
import org.code13k.thumbly.config.ChannelConfig;
import org.code13k.thumbly.model.config.channel.ChannelInfo;
import org.code13k.thumbly.service.api.controller.AppAPI;
import org.code13k.thumbly.service.api.controller.CacheAPI;
import org.code13k.thumbly.service.api.controller.ClusterAPI;
import org.code13k.thumbly.service.api.controller.ImageAPI;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;


public class ApiHttpServer extends AbstractVerticle {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(ApiHttpServer.class);

    // Port
    public static final int PORT = AppConfig.getInstance().getPort().getApiHttp();

    // API Controllers
    private final AppAPI mAppAPI = new AppAPI();
    private final ClusterAPI mClusterAPI = new ClusterAPI();
    private final ImageAPI mImageAPI = new ImageAPI();
    private final CacheAPI mCacheAPI = new CacheAPI();



    /**
     * Start
     */
    @Override
    public void start() throws Exception {
        super.start();
        mLogger.info("start()");

        // Init HTTP APIHttpServer
        HttpServerOptions httpServerOptions = new HttpServerOptions();
        httpServerOptions.setCompressionSupported(true);
        httpServerOptions.setPort(PORT);
        httpServerOptions.setIdleTimeout(5); // seconds
        HttpServer httpServer = vertx.createHttpServer(httpServerOptions);

        // Routing
        Router router = Router.router(vertx);
        setAppRouter(router);
        setClusterRouter(router);
        setImageRouter(router);
        setCacheRouter(router);


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
            mLogger.info("API HTTP Server");
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
     * Set app router
     */
    private void setAppRouter(Router router) {
        // GET /app/env
        router.route().method(HttpMethod.GET).path("/app/env").handler(routingContext -> {
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    responseHttpOK(routingContext, mAppAPI.env());
                }
            });
        });
        // GET /app/status
        router.route().method(HttpMethod.GET).path("/app/status").handler(routingContext -> {
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    responseHttpOK(routingContext, mAppAPI.status());
                }
            });
        });
        // GET /app/config
        router.route().method(HttpMethod.GET).path("/app/config").handler(routingContext -> {
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    responseHttpOK(routingContext, mAppAPI.config());
                }
            });
        });
        // GET /app/hello
        router.route().method(HttpMethod.GET).path("/app/hello").handler(routingContext -> {
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    responseHttpOK(routingContext, mAppAPI.hello());
                }
            });
        });
        // GET /app/ping
        router.route().method(HttpMethod.GET).path("/app/ping").handler(routingContext -> {
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    responseHttpOK(routingContext, mAppAPI.ping());
                }
            });
        });
    }

    /**
     * Set cluster router
     */
    private void setClusterRouter(Router router) {
        // GET /cluster/status
        router.route().method(HttpMethod.GET).path("/cluster/status").handler(routingContext -> {
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    responseHttpOK(routingContext, mClusterAPI.status());
                }
            });
        });
    }

    /**
     * Set cache router
     */
    private void setCacheRouter(Router router) {
        // DELETE /cache/origin/:channel/:path
        router.route().method(HttpMethod.DELETE).path("/cache/origin/:channel/*").handler(routingContext -> {
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    final HttpMethod method = routingContext.request().method();
                    final String prefixString = "/cache/origin/";
                    final String channelString = routingContext.request().getParam("channel");
                    final String requestUrl = routingContext.request().uri();
                    final String pathString = parsePath(requestUrl, prefixString, channelString);

                    if (StringUtils.isEmpty(pathString) == true) {
                        responseHttpError(routingContext, 400, "Bad Request (Invalid Path)");
                        return;
                    }
                    ChannelInfo channelInfo = ChannelConfig.getInstance().getChannelInfo(channelString);
                    if (channelInfo == null) {
                        responseHttpError(routingContext, 400, "Bad Request. (Invalid Channel)");
                        return;
                    }
                    String originUrl = channelInfo.getBaseUrl() + "/" + pathString;
                    mLogger.debug("originUrl = " + originUrl);
                    responseHttpOK(routingContext, mCacheAPI.deleteOrigin(originUrl));
                }
            });
        });
    }

    /**
     * Set image router
     */
    private void setImageRouter(Router router) {
        // GET /image/secret/:secret_path
        router.route().path("/image/secret/:secret_path").handler(routingContext -> {
            routingContext.request().endHandler(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    final String secretPathString = routingContext.request().getParam("secret_path");
                    mLogger.trace("secretPathString = " + secretPathString);
                    mImageAPI.getOriginUrl(secretPathString, new Consumer<String>() {
                        @Override
                        public void accept(String resultString) {
                            if (StringUtils.isEmpty(resultString) == true) {
                                responseHttpError(routingContext, 400, "Bad Request (Invalid Secret Path)");
                            } else {
                                responseHttpOK(routingContext, resultString);
                            }
                        }
                    });
                }
            });
        });

        // POST /image/secret
        //
        // {
        //   "data" : [
        //     {"originPath":"originPath1", "expires":10},
        //     {"originPath":"originPath2", "expires":20}
        //   ]
        // }
        router.route().method(HttpMethod.POST).path("/image/secret").handler(routingContext -> {
            routingContext.request().setExpectMultipart(true);
            routingContext.request().bodyHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer event) {
                    // Get Body Data
                    String body = event.toString();

                    // Parse Parameters
                    ArrayList<HashMap<String, Object>> parameterList = new ArrayList<>();
                    try {
                        JsonObject jsonObject = new JsonObject(body);
                        JsonArray jsonArray = jsonObject.getJsonArray("data");
                        for (int i = 0; i < jsonArray.size(); i++) {
                            HashMap<String, Object> parameter = new HashMap<>();
                            JsonObject param = jsonArray.getJsonObject(i);

                            // Secret Path
                            String secretPath = param.getString("secretPath", "");
                            parameter.put("secretPath", secretPath);

                            // Origin Path
                            String originPath = param.getString("originPath", "");
                            parameter.put("originPath", originPath);

                            // Expired
                            int expires = 0;
                            try {
                                expires = param.getInteger("expires", 0);
                            } catch (Exception e) {
                                String temp = param.getString("expires", "0");
                                expires = Integer.valueOf(temp);
                            }
                            parameter.put("expires", expires);

                            // End
                            parameterList.add(parameter);
                            if (StringUtils.isEmpty(secretPath) || StringUtils.isEmpty(originPath) || expires == 0) {
                                responseHttpError(routingContext, 400, "Bad Request");
                                return;
                            }
                        }
                    } catch (Exception e) {
                        mLogger.error("error : " + e);
                    }

                    // End
                    mImageAPI.createSecretUrl(parameterList, new Consumer<String>() {
                        @Override
                        public void accept(String result) {
                            mLogger.trace("result : " + result);
                            responseHttpOK(routingContext, result);
                        }
                    });
                }
            });
        });
    }


    /**
     * Parse path
     */
    private String parsePath(String requestUrl, String prefix, String channel) {
        // Parse
        StringBuffer pathPrefixBuffer = new StringBuffer();
        pathPrefixBuffer.append(prefix);
        pathPrefixBuffer.append(channel);
        pathPrefixBuffer.append("/");
        String pathPrefix = pathPrefixBuffer.toString();
        int index = StringUtils.indexOf(requestUrl, pathPrefix);
        String path = StringUtils.substring(requestUrl, index + pathPrefix.length());

        // URL Decoding
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (Exception e) {
            mLogger.error("Failed to decode path string");
        }

        // End
        return path;
    }

    /**
     * Response HTTP 200 OK
     */
    private void responseHttpOK(RoutingContext routingContext, String message) {
        HttpServerResponse response = routingContext.response();
        response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        response.setStatusCode(200);
        response.setStatusMessage("OK");
        response.end(message);
        response.close();
    }

    /**
     * Response HTTP error status
     */
    private void responseHttpError(RoutingContext routingContext, int statusCode, String message) {
        HttpServerResponse response = routingContext.response();
        response.putHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        response.setStatusCode(statusCode);
        response.setStatusMessage(message);
        response.end(message);
        response.close();
    }
}
