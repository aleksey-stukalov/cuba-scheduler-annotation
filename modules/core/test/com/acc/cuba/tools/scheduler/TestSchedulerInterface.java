package com.acc.cuba.tools.scheduler;

/**
 * Created by Aleksey Stukalov on 28/09/2017.
 */
public interface TestSchedulerInterface {

    @SuppressWarnings("unused")
    void testCorrectScheduledTasks(String a, String b, String code);

    void testFailedScheduledTasks(String arg);
}
