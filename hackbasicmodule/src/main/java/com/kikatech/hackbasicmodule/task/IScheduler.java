package com.kikatech.hackbasicmodule.task;

/**
 * Created by xm on 17/4/12.
 */
public interface IScheduler {
    void schedule(Runnable runnable, long delay);
}
