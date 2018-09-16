package org.code13k.thumbly.web.client.model;

import io.vertx.core.MultiMap;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ResponseHeaders extends BasicModel {
    // Const
    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

    // Data
    private String cacheControl = null;
    private String contentLength = null;
    private String contentType = null;
    private String date = null;
    private String etag = null;
    private String expires = null;
    private String lastModified = null;


    /**
     * fromMultiMap()
     */
    public void fromMultiMap(MultiMap headers) {
        headers.forEach(header -> {
            String headerName = header.getKey().toLowerCase();
            String headerValue = header.getValue();
            if (headerName.equals("cache-control") == true) {
                setCacheControl(headerValue);
            } else if (headerName.equals("content-length") == true) {
                setContentLength(headerValue);
            } else if (headerName.equals("content-type") == true) {
                setContentType(headerValue);
            } else if (headerName.equals("date") == true) {
                setDate(headerValue);
            } else if (headerName.equals("etag") == true) {
                setEtag(headerValue);
            } else if (headerName.equals("expires") == true) {
                setExpires(headerValue);
            } else if (headerName.equals("last-modified") == true) {
                setLastModified(headerValue);
            }
        });
    }

    /**
     * Getter / Setter
     */
    public String getCacheControl() {
        return cacheControl;
    }

    public void setCacheControl(String cacheControl) {
        this.cacheControl = cacheControl;
    }

    public String getMaxAgeOfCacheControl() {
        try {
            String[] params = StringUtils.split(this.cacheControl, ",");
            if (params != null && params.length > 0) {
                for (int index = 0; index < params.length; index++) {
                    String param = params[index];
                    String[] keyValue = StringUtils.split(param, "=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        if (key.toLowerCase().equals("max-age") == true) {
                            return value;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Nothing
        }
        return null;
    }

    public boolean isPrivateOfCacheControl() {
        try {
            String[] params = StringUtils.split(this.cacheControl, ",");
            if (params != null && params.length > 0) {
                for (int index = 0; index < params.length; index++) {
                    String param = params[index];
                    if (param.equalsIgnoreCase("private") == true) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Nothing
        }
        return false;
    }

    public boolean isNoCacheOfCacheControl() {
        try {
            String[] params = StringUtils.split(this.cacheControl, ",");
            if (params != null && params.length > 0) {
                for (int index = 0; index < params.length; index++) {
                    String param = params[index];
                    if (param.equalsIgnoreCase("no-cache") == true) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Nothing
        }
        return false;
    }

    public boolean isNoStoreOfCacheControl() {
        try {
            String[] params = StringUtils.split(this.cacheControl, ",");
            if (params != null && params.length > 0) {
                for (int index = 0; index < params.length; index++) {
                    String param = params[index];
                    if (param.equalsIgnoreCase("no-store") == true) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Nothing
        }
        return false;
    }

    public String getContentLength() {
        return contentLength;
    }

    public Long getContentLengthAsLong() {
        try {
            return Long.valueOf(this.contentLength);
        } catch (Exception e) {
            return 0L;
        }
    }

    public void setContentLength(String contentLength) {
        this.contentLength = contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Date getDateAsDate() {
        try {
            SimpleDateFormat format = new SimpleDateFormat(HTTP_DATE_FORMAT);
            return format.parse(this.date);
        } catch (Exception e) {
            return null;
        }
    }

    public long getDateAsMillis() {
        Date date = getDateAsDate();
        if (date != null) {
            return date.getTime();
        }
        return 0L;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public Date getExpiresAsDate() {
        try {
            SimpleDateFormat format = new SimpleDateFormat(HTTP_DATE_FORMAT);
            return format.parse(this.expires);
        } catch (Exception e) {
            return null;
        }
    }

    public long getExpiresAsMillis() {
        Date date = getExpiresAsDate();
        if (date != null) {
            return date.getTime();
        }
        return 0L;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public void setLastModifiedTimeMillis(long lastModifiedTimeMillis){
        SimpleDateFormat lastModifiedFormat = new SimpleDateFormat(HTTP_DATE_FORMAT);
        lastModifiedFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String lastModifiedString = lastModifiedFormat.format(lastModifiedTimeMillis);
        this.lastModified = lastModifiedString;
    }

    public Date getLastModifiedAsDate() {
        try {
            SimpleDateFormat format = new SimpleDateFormat(HTTP_DATE_FORMAT);
            return format.parse(this.lastModified);
        } catch (Exception e) {
            return null;
        }
    }

    public long getLastModifiedAsMillis() {
        Date date = getLastModifiedAsDate();
        if (date != null) {
            return date.getTime();
        }
        return 0L;
    }
}
