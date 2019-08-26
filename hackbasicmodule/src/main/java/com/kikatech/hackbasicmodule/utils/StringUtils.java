package com.kikatech.hackbasicmodule.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xm on 17/4/13.
 */

public class StringUtils {
    private StringUtils(){}


    public static String[] splitToNonEmptyStrings(@NonNull String str, @NonNull String regex) {
        String[] strings = str.split(regex);
        List<String> list = new ArrayList<>(strings.length);
        for (String string : strings) {
            String trimedStr = string.trim();
            if (!TextUtils.isEmpty(trimedStr)) {
                list.add(trimedStr);
            }
        }
        return list.toArray(new String[list.size()]);
    }
}
