package org.code13k.thumbly.service.api.controller;

import org.apache.commons.lang3.StringUtils;
import org.code13k.thumbly.business.ClusteredSecretUrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ImageAPI extends BasicAPI {

    /**
     * Create secret url
     */
    public void createSecretUrl(ArrayList<HashMap<String, Object>> parameterList, Consumer<String> consumer) {
        ArrayList<HashMap<String, Object>> resultList = new ArrayList<>();
        AtomicInteger processingCount = new AtomicInteger(parameterList.size());

        parameterList.forEach(parameter -> {
            HashMap<String, Object> resultItem = new HashMap<>();
            String originPath = (String) parameter.get("originPath");
            long expires = (long) parameter.get("expires");
            ClusteredSecretUrl.getInstance().set(originPath, expires, new Consumer<String>() {
                @Override
                public void accept(String secretPath) {
                    if (StringUtils.isEmpty(secretPath) == true) {
                        secretPath = "";
                    }
                    resultItem.put("originPath", originPath);
                    resultItem.put("secretPath", secretPath);
                    resultList.add(resultItem);
                    if (processingCount.decrementAndGet() == 0) {
                        if (consumer != null) {
                            consumer.accept(toResultJsonString(resultList));
                        }
                    }
                }
            });
        });
    }

    /**
     * Get origin url
     */
    public void getOriginUrl(String secretPath, Consumer<String> consumer) {
        ClusteredSecretUrl.getInstance().get(secretPath, new Consumer<String>() {
            @Override
            public void accept(String originPath) {
                if (consumer != null) {
                    consumer.accept(toResultJsonString(originPath));
                }
            }
        });
    }
}
