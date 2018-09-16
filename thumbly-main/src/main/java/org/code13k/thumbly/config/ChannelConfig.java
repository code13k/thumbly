package org.code13k.thumbly.config;


import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.lib.Util;
import org.code13k.thumbly.model.config.channel.AwsS3Info;
import org.code13k.thumbly.model.config.channel.ChannelInfo;
import org.code13k.thumbly.model.config.channel.HttpInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class ChannelConfig extends BasicConfig {
    // Logger
    private static final Logger mLogger = LoggerFactory.getLogger(ChannelConfig.class);

    // Data
    private HashMap<String, ChannelInfo> mConfigChannelByKey = new HashMap<>();

    /**
     * Channel Type
     */
    public class ChannelType {
        public static final String HTTP = "http";
        public static final String AWS_S3 = "aws_s3";
    }

    /**
     * Singleton
     */
    private static class SingletonHolder {
        static final ChannelConfig INSTANCE = new ChannelConfig();
    }

    public static ChannelConfig getInstance() {
        return ChannelConfig.SingletonHolder.INSTANCE;
    }

    /**
     * Constructor
     */
    private ChannelConfig() {
        mLogger.trace("ChannelConfig()");
    }

    /**
     * Get channel list by key
     */
    public ChannelInfo getChannelInfo(String channelKey) {
        if (StringUtils.isEmpty(channelKey) == false) {
            ChannelInfo channelInfo = mConfigChannelByKey.get(channelKey);
            mLogger.trace("channelList = " + channelInfo);
            return channelInfo;
        }
        return null;
    }

    @Override
    protected String getDefaultConfigFilename() {
        return "default_channel_config.yml";
    }

    @Override
    protected String getConfigFilename() {
        return "channel_config.yml";
    }

    @Override
    protected boolean loadConfig(final String content, final String filePath) {
        try {
            Yaml yaml = new Yaml();
            LinkedHashMap<String, LinkedHashMap> yamlObject = yaml.load(content);
            mLogger.trace("yamlObject class name = " + yamlObject.getClass().getName());
            mLogger.trace("yamlObject = " + yamlObject);

            // Get data
            yamlObject.forEach((key, value) -> {
                mLogger.trace("value class name = " + value.getClass().getName());
                mLogger.trace("[" + key + "]");

                // Type
                LinkedHashMap valueObject = value;
                String type = (String) valueObject.get("type");
                int browserCacheExpiration = Util.toSeconds( (String)valueObject.getOrDefault("browser_cache_expiration", ""));
                boolean normalUrlEnabled = (Boolean) valueObject.getOrDefault("normal_url_enabled", true);
                boolean secretUrlEnabled = (Boolean) valueObject.getOrDefault("secret_url_enabled", true);

                // Http
                if (type.equalsIgnoreCase(ChannelType.HTTP)) {
                    String baseUrl = (String) valueObject.getOrDefault("base_url", "");

                    // Set Http
                    HttpInfo httpInfo = new HttpInfo();
                    httpInfo.setBrowserCacheExpiration(browserCacheExpiration);
                    httpInfo.setNormalUrlEnabled(normalUrlEnabled);
                    httpInfo.setSecretUrlEnabled(secretUrlEnabled);
                    httpInfo.setType(ChannelType.HTTP);
                    httpInfo.setBaseUrl(baseUrl);

                    // Check validation
                    if (StringUtils.isEmpty(httpInfo.getBaseUrl()) == true) {
                        mLogger.error("Invalid http channel (base_url is invalid)");
                    } else {
                        mConfigChannelByKey.put(key, httpInfo);
                    }
                }

                // Aws S3
                else if (type.equalsIgnoreCase(ChannelType.AWS_S3)) {
                    String accessKey = (String) valueObject.getOrDefault("access_key", "");
                    String secretKey = (String) valueObject.getOrDefault("secret_key", "");
                    String region = (String) valueObject.getOrDefault("region", "");
                    String bucket = (String) valueObject.getOrDefault("bucket", "");

                    // Set Aws S3
                    AwsS3Info awsS3Info = new AwsS3Info();
                    awsS3Info.setBrowserCacheExpiration(browserCacheExpiration);
                    awsS3Info.setNormalUrlEnabled(normalUrlEnabled);
                    awsS3Info.setSecretUrlEnabled(secretUrlEnabled);
                    awsS3Info.setType(ChannelType.AWS_S3);
                    awsS3Info.setAccessKey(accessKey);
                    awsS3Info.setSecretKey(secretKey);
                    awsS3Info.setRegion(region);
                    awsS3Info.setBucket(bucket);

                    // Check validation
                    if (StringUtils.isEmpty(awsS3Info.getAccessKey()) == true) {
                        mLogger.error("Invalid aws_s3 channel (access_key is invalid)");
                    } else if (StringUtils.isEmpty(awsS3Info.getSecretKey()) == true) {
                        mLogger.error("Invalid aws_s3 channel (secret_key is invalid)");
                    } else if (StringUtils.isEmpty(awsS3Info.getRegion()) == true) {
                        mLogger.error("Invalid aws_s3 channel (region is invalid)");
                    } else if (StringUtils.isEmpty(awsS3Info.getBucket()) == true) {
                        mLogger.error("Invalid aws_s3 channel (bucket is invalid)");
                    } else {
                        mConfigChannelByKey.put(key, awsS3Info);
                    }
                }

                // Not supported
                else {
                    mLogger.warn("Not supported type : " + type);
                }
            });
        } catch (Exception e) {
            mLogger.error("Failed to load config file", e);
            return false;
        }
        return true;
    }

    @Override
    protected void logging() {
        // Begin
        mLogger.info("------------------------------------------------------------------------");
        mLogger.info("Channel Configuration");
        mLogger.info("------------------------------------------------------------------------");

        // Config File Path
        mLogger.info("Config file path = " + getConfigFilename());

        // Channel
        mConfigChannelByKey.forEach((key, value) -> {
            mLogger.info(key + " = " + value);
        });

        // End
        mLogger.info("------------------------------------------------------------------------");
    }
}
