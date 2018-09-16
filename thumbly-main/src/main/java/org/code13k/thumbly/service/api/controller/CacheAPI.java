package org.code13k.thumbly.service.api.controller;

import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.web.client.CachedWebClient;
import org.code13k.thumbly.web.client.model.WebData;

import java.util.HashMap;
import java.util.Map;

public class CacheAPI extends BasicAPI {
    /**
     * Delete origin image
     */
    public String deleteOrigin(String url) {
        boolean result = false;

        if (StringUtils.isEmpty(url) == false) {
            CachedWebClient.getInstance().deleteCache(url);
            result = true;
        }
        return toResultJsonString(result);
    }

    /**
     * Get origin image information
     */
    public String getOrigin(String url){
        Map<String, Object> result = null;
        if (StringUtils.isEmpty(url) == false) {
            WebData webData = CachedWebClient.getInstance().getCache(url);
            result = webData.toMap();
        }
        return toResultJsonString(result);
    }

}
