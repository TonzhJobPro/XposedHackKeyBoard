package com.kikatech.hackbasicmodule.utils;

import android.util.Log;

/**
 * Created by xm on 17/4/13.
 */

public class Logger {

    private Logger(){}

    public static final String TAG = "Xposed_hack_module";
    public static final String TAG_PRINT = TAG + "_output";

    public static void i(String msg) {
        i(msg, new Object());
    }

    public static void i(String tag, String msg) {
        i(tag, msg, new Object());
    }

    public static void i(String format, Object... args) {
       i(TAG, format, args);
    }

    public static void i(String tag, String format, Object... args) {
        String msg = String.format(format, args);
        Log.i(tag, msg);
    }

    public static void v(String msg) {
        v(msg, new Object[0]);
    }

    public static void v(String format, Object... args) {
        String msg = String.format(format, args);
        Log.v(TAG, msg);
    }


    public static void e(String msg) {
        e(msg, new Object());
    }

    public static void e(String msg, Throwable tr) {
        e(TAG, tr, msg, new Object());
    }

    public static void e(String format, Object... args) {
        e(TAG, null, format, args);
    }

    public static void e(Throwable tr) {
        if (tr != null) {
            e(tr.getMessage(), tr);
        }
    }

    public static void e(String tag, Throwable tr, String format, Object... args) {
        String msg = String.format(format, args);
        Log.e(tag, msg, tr);
    }
}
