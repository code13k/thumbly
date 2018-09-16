package org.code13k.thumbly.web.client.model;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;

public class WebData extends BasicModel {
    private String url;
    private String filePath;
    private long fileSize;
    private long expiredTimeSeconds;
    private ResponseHeaders responseHeaders;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        try {
            this.filePath = filePath;
            if (StringUtils.isEmpty(this.filePath) == false) {
                if (Files.exists(Paths.get(this.filePath)) == true) {
                    this.fileSize = Files.size(Paths.get(this.filePath));
                }
            }
        } catch (Exception e) {
            this.filePath = null;
            this.fileSize = 0;
        }
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public long getExpiredTimeSeconds() {
        return expiredTimeSeconds;
    }

    public void setExpiredTimeSeconds(long expiredTimeSeconds) {
        this.expiredTimeSeconds = expiredTimeSeconds;
    }

    public ResponseHeaders getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(ResponseHeaders responseHeaders) {
        this.responseHeaders = responseHeaders;
    }
}
