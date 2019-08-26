package com.kikatech.hackbasicmodule.bean;

import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by xm on 17/4/14.
 */
public class Candidate {
    @Nullable
    private View view;
    private String value;
    @Nullable
    private String tag;

    public Candidate(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(obj == null || !(obj instanceof Candidate)){
            return false;
        }
        Candidate c = (Candidate)obj;
        if(value != null && value.equals(c.getValue()) ||
                c.getValue() != null && c.getValue().equals(value)){
            return true;
        }
        return super.equals(obj);
    }

    //    public Candidate(String value, View view) {
//        this.value = value;
//        this.view = view;
//    }
//
//    public Candidate(View view, String value, String tag) {
//        this.view = view;
//        this.value = value;
//        this.tag = tag;
//    }

//    public void setView(@Nullable View view) {
//        this.view = view;
//    }
//
//    public void setValue(String value) {
//        this.value = value;
//    }
//
//    public void setTag(@Nullable String tag) {
//        this.tag = tag;
//    }

//    @Nullable
//    public View getView() {
//        return view;
//    }

    public String getValue() {
        return value;
    }

//    @Nullable
//    public String getTag() {
//        return tag;
//    }

    @Override
    public String toString() {
        return "Candidate{" +
                "view=" + view +
                ", value='" + value + '\'' +
                ", tag='" + tag + '\'' +
                '}';
    }
}
