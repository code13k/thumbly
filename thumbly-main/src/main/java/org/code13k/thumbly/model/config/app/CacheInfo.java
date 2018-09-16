package org.code13k.thumbly.model.config.app;

public class CacheInfo {
    private String rootDirectory;
    private long totalSizeOfOriginImages;
    private long totalSizeOfThumbnailImages;

    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public long getTotalSizeOfOriginImages() {
        return totalSizeOfOriginImages;
    }

    public void setTotalSizeOfOriginImages(long totalSizeOfOriginImages) {
        this.totalSizeOfOriginImages = totalSizeOfOriginImages;
    }

    public long getTotalSizeOfThumbnailImages() {
        return totalSizeOfThumbnailImages;
    }

    public void setTotalSizeOfThumbnailImages(long totalSizeOfThumbnailImages) {
        this.totalSizeOfThumbnailImages = totalSizeOfThumbnailImages;
    }
}
