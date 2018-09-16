package org.code13k.thumbly.service.api.controller;

import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.business.SecretUrlManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ImageAPI extends BasicAPI {

    /**
     * Create secret url
     */
    public String createSecretUrl(ArrayList<HashMap<String, Object>> parameterList) {
        ArrayList<HashMap<String, Object>> resultList = new ArrayList<>();

        parameterList.forEach(parameter -> {
            HashMap<String, Object> resultItem = new HashMap<>();
            String secretPath = (String) parameter.get("secretPath");
            String originPath = (String) parameter.get("originPath");
            int expired = (int) parameter.get("expired");
            boolean result = SecretUrlManager.getInstance().set(secretPath, originPath, expired);
            resultItem.put("secretPath", secretPath);
            resultItem.put("originPath", originPath);
            resultItem.put("expired", expired);
            resultItem.put("result", result);
            resultList.add(resultItem);
        });
        return toResultJsonString(resultList);
    }

    /**
     * Get secret url
     */
    public String getSecretUrl(String secretPath) {
        Map<String, Object> result = null;
        String originPath = SecretUrlManager.getInstance().get(secretPath);
        if (StringUtils.isEmpty(originPath) == false) {
            long expirationTime = SecretUrlManager.getInstance().getExpiration(secretPath);
            result = new HashMap<>();
            result.put("originPath", originPath);
            result.put("expired", expirationTime);
        }
        return toResultJsonString(result);
    }
}
