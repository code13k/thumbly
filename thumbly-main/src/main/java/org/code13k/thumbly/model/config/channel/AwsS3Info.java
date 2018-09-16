package org.code13k.thumbly.model.config.channel;

public class AwsS3Info extends ChannelInfo {
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucket;

    @Override
    public String getBaseUrl() {
        return "https://s3." + region + ".amazonaws.com";
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
