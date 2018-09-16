package org.code13k.thumbly.image.processor.model;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class Command extends BasicModel {
    private Type type = null;
    private Size size = null;
    private Format format = null;
    private int quality = 100;


    /**
     * fromCommand
     */
    public void fromCommand(Command command) {
        if (command != null) {
            setType(command.getType());
            setSize(command.getSize());
            setFormat(command.getFormat());
            setQuality(command.getQuality());
        }
    }

    /**
     * Getter / Setter
     */
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isValidType(String type) {
        if (StringUtils.isEmpty(type) == false) {
            return EnumUtils.isValidEnum(Type.class, type.toUpperCase());
        }
        return false;
    }

    public void setType(String type) {
        if (isValidType(type) == true) {
            this.type = Type.valueOf(type.toUpperCase());
        }
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public boolean isValidFormat(String format) {
        if (StringUtils.isEmpty(format) == false) {
            return EnumUtils.isValidEnum(Format.class, format.toUpperCase());
        }
        return false;
    }

    public void setFormat(String format) {
        if (isValidFormat(format) == true) {
            this.format = Format.valueOf(format.toUpperCase());
        }
    }

    public Size getSize() {
        return this.size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public void setSize(int width, int height) {
        this.size = new Size(width, height);
    }

    /**
     * Parse Size String
     * <p>
     * ex) 100x100 -> Size(100,100)
     * ex) 100x0 -> Size(100,0)
     * ex) 0x100 -> Size(0,100)
     *
     * @param sizeString size string
     * @return Size Object
     */
    public void setSize(String sizeString) {
        sizeString = sizeString.toLowerCase();
        int indexX = sizeString.indexOf('x');
        if (indexX > 0) {
            String[] sizeArray = StringUtils.split(sizeString, 'x');
            if (sizeArray.length == 2) {
                Size resultSize = new Size();
                String sizeWidthString = sizeArray[0];
                String sizeHeightString = sizeArray[1];
                resultSize.width = NumberUtils.toInt(sizeWidthString, -1);
                resultSize.height = NumberUtils.toInt(sizeHeightString, -1);
                if (resultSize.width < 0 || resultSize.height < 0) {
                    this.size = null;
                } else if (resultSize.width == 0 && resultSize.height == 0) {
                    this.size = null;
                } else {
                    this.size = resultSize;
                }
                return;
            }
        }
        this.size = null;
    }

    public void setQuality(String quality){
        setQuality(NumberUtils.toInt(quality, 100));
    }

    public void setQuality(int quality){
        this.quality = Math.min(100, Math.max(0,quality));
    }

    public int getQuality(){
        return quality;
    }

    /**
     * Enum Type
     */
    public enum Type {
        ORIGIN(0), RESIZE(10), THUMB(20), THUMB_TOP(21), THUMB_RIGHT(22), THUMB_BOTTOM(23), THUMB_LEFT(24), CROP(30);
        private int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    /**
     * Enum Format
     */
    public enum Format {
        ORIGIN(0), JPG(1), JPEG(2), PNG(3), GIF(4), WEBP(5);
        private int value;

        Format(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }
}
