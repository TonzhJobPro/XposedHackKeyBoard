package com.kikatech.hackbasicmodule.task;

/**
 * Created by xm on 17/4/12.
 */

public abstract class AbstractTask implements Runnable{
    private final long mDelay;

    AbstractTask(long delay) {
        mDelay = delay;
    }

    abstract boolean synRun(IScheduler scheduler);

    public long getDelay() {
        return mDelay;
    }
}
