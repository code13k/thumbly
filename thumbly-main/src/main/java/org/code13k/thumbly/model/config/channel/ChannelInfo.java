package org.code13k.thumbly.model.config.channel;

import org.code13k.thumbly.model.BasicModel;

public abstract class ChannelInfo extends BasicModel {
    private int browserCacheExpiration;
    private boolean normalUrlEnabled;
    private boolean secretUrlEnabled;
    private String type;



    public int getBrowserCacheExpiration() {
        return browserCacheExpiration;
    }

    public void setBrowserCacheExpiration(int browserCacheExpiration) {
        this.browserCacheExpiration = browserCacheExpiration;
    }

    public boolean isNormalUrlEnabled() {
        return normalUrlEnabled;
    }

    public void setNormalUrlEnabled(boolean normalUrlEnabled) {
        this.normalUrlEnabled = normalUrlEnabled;
    }

    public boolean isSecretUrlEnabled() {
        return secretUrlEnabled;
    }

    public void setSecretUrlEnabled(boolean secretUrlEnabled) {
        this.secretUrlEnabled = secretUrlEnabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public abstract String getBaseUrl();
}

