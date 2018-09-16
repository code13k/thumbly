package org.code13k.thumbly.image.processor.model;

import java.util.function.Consumer;

public class Operation extends BasicModel {
    private String filePath;
    private Command command;
    private Consumer<String> consumer;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Command getCommand(){ return command; }

    public void setCommand(Command command){
        this.command = command;
    }

    public Consumer<String> getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer<String> consumer) {
        this.consumer = consumer;
    }
}
