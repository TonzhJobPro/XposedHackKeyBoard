package com.kikatech.bean;

/**
 * Created by xm on 17/4/17.
 */

public class KeyInfo {
    private int x;
    private int y;
    private String code;

    public KeyInfo(int x, int y, String code) {
        this.x = x;
        this.y = y;
        this.code = code;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
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

    public String getCode() {
        return code;
    }
}
