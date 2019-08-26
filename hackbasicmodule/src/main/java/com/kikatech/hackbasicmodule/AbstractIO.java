package com.kikatech.hackbasicmodule;

import com.kikatech.hackbasicmodule.utils.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by xuemin on 16/12/20.
 */
public abstract class AbstractIO {
    Map<String, Closeable> closeableMap = new HashMap<String, Closeable>();

    public BufferedReader getReader(String filePath) {
        BufferedReader reader = IOUtils.getReader(filePath);
        closeableMap.put(filePath + UUID.randomUUID(), reader);
        return reader;
    }

    public BufferedWriter getWriter(String filePath) {
        BufferedWriter writer = IOUtils.getWriter(filePath);
        closeableMap.put(filePath + UUID.randomUUID(), writer);
        return writer;
    }

    public PrintStream getPrintStream(String filePath, boolean append) {
        PrintStream printStream = IOUtils.getPrintStream(filePath, append);
        closeableMap.put(filePath + UUID.randomUUID(), printStream);
        return printStream;
    }

    public PrintStream getPrintStream(String filePath) {
        return getPrintStream(filePath, true);
    }

    public void close() {
        for (Map.Entry<String, Closeable> entry : closeableMap.entrySet()) {
            IOUtils.close(entry.getValue());
        }
        closeableMap.clear();
    }

    protected void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
