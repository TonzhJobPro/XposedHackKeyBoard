package com.kikatech.hackbasicmodule.task;

import android.content.Context;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by xm on 17/4/13.
 */

@RunWith(AndroidJUnit4.class)
public class SynTaskTest {

    private static final String TAG = "SynTaskTest";


    @Test
    public void synRunTest() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        Handler mHandler = new Handler(appContext.getMainLooper());

        boolean oneResult = new SynTask(300) {
            @Override
            public void run() {
                Log.i(TAG, "synRunTest 1");
            }
        }.synRun(new HandlerScheduler(mHandler));

        boolean twoResult = new SynTask(100) {
            @Override
            public void run() {
                Log.i(TAG, "synRunTest 2");
            }
        }.synRun(new HandlerScheduler(mHandler));

        boolean threeResult = new SynTask(50) {
            @Override
            public void run() {
                Log.i(TAG, "synRunTest 3");
            }
        }.synRun(new HandlerScheduler(mHandler));

        Assert.assertEquals(oneResult, true);
        Assert.assertEquals(twoResult, true);
        Assert.assertEquals(threeResult, true);
    }
}
