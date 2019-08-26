package com.kikatech.bean;

/**
 * Created by xm on 17/4/18.
 */

public class KeyboardPointerInfo {
    private int x;
    private int y;
    private String type;
    private String code;

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public KeyInfo getKeyInfo() {
        return new KeyInfo(getX(), getY(), getCode());
    }

    public KeyInfo getKeyInfo(int y) {
        return new KeyInfo(getX(), getY() - y, getCode());
    }

    @Override
    public String toString() {
        return "KeyboardPointerInfo{" +
                "x='" + x + '\'' +
                ", y='" + y + '\'' +
                ", type='" + type + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
