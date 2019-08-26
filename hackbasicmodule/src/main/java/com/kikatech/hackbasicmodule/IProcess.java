package com.kikatech.hackbasicmodule;

import com.kikatech.hackbasicmodule.task.IScheduler;

/**
 * Created by xm on 17/4/13.
 */

public interface IProcess {
    void run(IScheduler scheduler);
}
