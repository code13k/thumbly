package org.code13k.thumbly.model;

import java.util.UUID;

public class Procedure extends BasicModel {
    private String id;
    private String method;
    private Object params;

    public String getId() {
        return id;
    }

    public void setId() {
        this.id = UUID.randomUUID().toString();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }
}
