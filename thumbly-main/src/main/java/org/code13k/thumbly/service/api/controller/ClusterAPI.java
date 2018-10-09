package org.code13k.thumbly.service.api.controller;

import org.code13k.thumbly.app.Cluster;

public class ClusterAPI extends BasicAPI {
    /**
     * Status
     */
    public String status() {
        return toResultJsonString(Cluster.getInstance().values());
    }
}
