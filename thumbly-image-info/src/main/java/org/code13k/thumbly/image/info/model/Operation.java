package org.code13k.thumbly.image.info.model;

import java.util.function.Consumer;

public class Operation extends BasicModel {
    private String filePath;
    private Consumer<ImageInfo> consumer;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Consumer<ImageInfo> getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer<ImageInfo> consumer) {
        this.consumer = consumer;
    }
}
