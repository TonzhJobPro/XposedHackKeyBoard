package com.kikatech.hackbasicmodule.bean;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xm on 17/4/17.
 */

public class KeyboardConfig {
    private String packageName;
    private String inputmethodServiceClazz;
    private String mainKeyboardViewClazz;
    private String candidateViewClazz;
    private String keyboardType;
    private List<KeyInfo> keys;

    public KeyboardConfig(String packageName, String inputmethodServiceClazz, String mainKeyboardViewClazz, String candidateViewClazz, String keyboardType, List<KeyInfo> keys) {
        this.packageName = packageName;
        this.inputmethodServiceClazz = inputmethodServiceClazz;
        this.mainKeyboardViewClazz = mainKeyboardViewClazz;
        this.candidateViewClazz = candidateViewClazz;
        this.keyboardType = keyboardType;
        this.keys = keys;
    }


    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setInputmethodServiceClazz(String inputmethodServiceClazz) {
        this.inputmethodServiceClazz = inputmethodServiceClazz;
    }

    public void setMainKeyboardViewClazz(String mainKeyboardViewClazz) {
        this.mainKeyboardViewClazz = mainKeyboardViewClazz;
    }

    public void setCandidateViewClazz(String candidateViewClazz) {
        this.candidateViewClazz = candidateViewClazz;
    }

    public void setKeyboardType(String keyboardType) {
        this.keyboardType = keyboardType;
    }

    public void setKeys(List<KeyInfo> keys) {
        this.keys = keys;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getInputmethodServiceClazz() {
        return inputmethodServiceClazz;
    }

    public String getMainKeyboardViewClazz() {
        return mainKeyboardViewClazz;
    }

    public String getCandidateViewClazz() {
        return candidateViewClazz;
    }

    public String getKeyboardType() {
        return keyboardType;
    }

    public List<KeyInfo> getKeys() {
        return keys;
    }

    public KeyInfo getKeyInfo(Character character) {
        for (KeyInfo keyInfo : getKeys()) {
            if (keyInfo.getCode().equals(character.toString())) {
                return keyInfo;
            }
        }
        return null;
    }

    public KeyInfo getSpaceKey() {
        return getFounctionKey("space");
    }

    public KeyInfo getDeleteKey() {
        return getFounctionKey("delete");
    }

    public KeyInfo getShiftKey() {
        return getFounctionKey("shift");
    }

    public KeyInfo getEnterKey() {
        return getFounctionKey("enter");
    }

    public KeyInfo getDoubleQuotationKey() {
        return getFounctionKey("doublequotation");
    }

    public KeyInfo getQuotationKey() {
        return getFounctionKey("quotation");
    }

    public KeyInfo getSymbolKey() {
        return getFounctionKey("symbol");
    }

    private KeyInfo getFounctionKey(String code) {
        for (KeyInfo keyInfo : getKeys()) {
            if (keyInfo.getCode().equals(code)) {
                return keyInfo;
            }
        }
        return null;
    }

    public boolean isNormalChar(Character character) {
        List<KeyInfo> keys = getKeys();
        for (KeyInfo key : keys) {
            if (!isFounctionKey(key) && key.getCode().equals(character)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFounctionKey(KeyInfo keyInfo) {
        switch (keyInfo.getCode()) {
            case "shift":
            case "delete":
            case "space":
            case "enter":
                return true;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return "KeyboardConfig{" +
                "packageName='" + packageName + '\'' +
                ", inputmethodServiceClazz='" + inputmethodServiceClazz + '\'' +
                ", mainKeyboardViewClazz='" + mainKeyboardViewClazz + '\'' +
                ", candidateViewClazz='" + candidateViewClazz + '\'' +
                ", keys=" + Arrays.toString(keys.toArray()) +
                '}';
    }

}
