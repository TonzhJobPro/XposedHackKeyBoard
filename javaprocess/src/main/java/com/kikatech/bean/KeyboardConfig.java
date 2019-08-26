package com.kikatech.bean;

import java.util.List;

/**
 * Created by xm on 17/4/17.
 */

public class KeyboardConfig {
    private String packageName;
    private String inputmethodServiceClazz;
    private String mainKeyboardViewClazz;
    private String candidateViewClazz;
    private List<KeyInfo> keys;

    public KeyboardConfig(String packageName, String inputmethodServiceClazz, String mainKeyboardViewClazz, String candidateViewClazz, List<KeyInfo> keys) {
        this.packageName = packageName;
        this.inputmethodServiceClazz = inputmethodServiceClazz;
        this.mainKeyboardViewClazz = mainKeyboardViewClazz;
        this.candidateViewClazz = candidateViewClazz;
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

    public List<KeyInfo> getKeys() {
        return keys;
    }

    @Override
    public String toString() {
        return "KeyboardConfig{" +
                "packageName='" + packageName + '\'' +
                ", inputmethodServiceClazz='" + inputmethodServiceClazz + '\'' +
                ", mainKeyboardViewClazz='" + mainKeyboardViewClazz + '\'' +
                ", candidateViewClazz='" + candidateViewClazz + '\'' +
                ", keys=" + keys +
                '}';
    }
}
