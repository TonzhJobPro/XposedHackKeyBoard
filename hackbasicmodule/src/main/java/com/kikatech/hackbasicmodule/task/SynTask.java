package com.kikatech.hackbasicmodule.task;

import java.util.concurrent.CountDownLatch;

/**
 * Created by xm on 17/4/12.
 */

public abstract class SynTask extends AbstractTask {

    public SynTask(long delay) {
        super(delay);
    }

    public boolean synRun(IScheduler scheduler) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    SynTask.this.run();
                } finally {
                    countDownLatch.countDown();
                }
            }
        }, getDelay());
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            return false;
        } finally {
            return true;
        }
    }
}
