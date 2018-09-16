package org.code13k.thumbly.model.config.channel;

public class HttpInfo extends ChannelInfo {
    private String baseUrl;

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
