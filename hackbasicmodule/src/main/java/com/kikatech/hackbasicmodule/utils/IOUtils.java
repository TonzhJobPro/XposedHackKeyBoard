package com.kikatech.hackbasicmodule.utils;

import android.support.annotation.NonNull;

import java.io.*;
/**
 * Created by xuemin on 16/12/19.
 */
public class IOUtils {
    private IOUtils(){}

    public static void close(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException e) {
                // do nothing
            }
    }

    public static BufferedReader getReader(@NonNull String filePath) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))));
        } catch (FileNotFoundException e) {
            Logger.e(e);
        } finally {
            return br;
        }
    }


    public static BufferedWriter getWriter(@NonNull String filePath) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(new File(filePath)));
        } catch (IOException e) {
            Logger.e(e);
        } finally {
            return bw;
        }
    }

    public static PrintStream getPrintStream(@NonNull String filePath, boolean append) {
        PrintStream printStream = null;
        try {
            printStream = new PrintStream(new FileOutputStream(new File(filePath), append));
        } catch (FileNotFoundException e) {
            Logger.e(e);
        } finally {
            return printStream;
        }
    }

    public static PrintStream getPrintStream(@NonNull String filePath) {
        return getPrintStream(filePath, true);
    }
}
