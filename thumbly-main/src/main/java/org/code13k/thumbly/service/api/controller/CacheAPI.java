package org.code13k.thumbly.service.api.controller;

import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.business.ClusteredProcedure;

public class CacheAPI extends BasicAPI {
    /**
     * Delete origin image
     */
    public String deleteOrigin(String url) {
        boolean result = false;

        if (StringUtils.isEmpty(url) == false) {
            ClusteredProcedure.getInstance().deleteCache(url);
            result = true;
        }
        return toResultJsonString(result);
    }
}
