package org.code13k.thumbly.image.processor.model;

public class Size extends BasicModel {
    public int width;
    public int height;

    public Size(){
        this.width = 0;
        this.height = 0;
    }

    public Size(int w, int h){
        this.width = w;
        this.height = h;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
