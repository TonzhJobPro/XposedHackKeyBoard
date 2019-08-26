package com.kikatech.hackbasicmodule.task;

import android.os.Handler;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by xm on 17/4/12.
 */

public class HandlerScheduler implements IScheduler {

    private final Handler mHandler;

    public HandlerScheduler(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void schedule(Runnable runnable, long delay) {
        mHandler.postDelayed(runnable, MILLISECONDS.toMillis(delay));
    }
}
