package org.code13k.thumbly.image.info.model;

public class ImageInfo extends BasicModel {
    private int orientation = 1;
    private int sizeWidth = 0;
    private int sizeHeight = 0;

    /**
     * Getter / Setter
     */
    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public void setSize(int width, int height) {
        this.sizeWidth = width;
        this.sizeHeight = height;
    }

    public int getSizeWidth() {
        return sizeWidth;
    }

    public int getSizeHeight() {
        return sizeHeight;
    }
}