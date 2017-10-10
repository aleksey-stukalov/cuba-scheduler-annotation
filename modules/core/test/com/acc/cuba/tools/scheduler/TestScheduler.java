package com.acc.cuba.tools.scheduler;

import com.acc.cuba.tools.scheduler.core.annotation.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Created by Aleksey Stukalov on 27/09/2017.
 */
@Component("scheduler_TestScheduler")
public class TestScheduler implements TestSchedulerInterface {

    @Inject
    private Logger log;

    @Override
    @ScheduledBeanMethod(
            code = "test-periodical-scheduler",
            userName = "admin",
            isSingleton = true,
            isActive = true,
            logStart = true,
            logFinish = true,
            timeout = 5,
            timeFrame = 10,
            description = "Test periodical scheduler",
            permittedServers = "localhost:8080/scheduler-core",
            methodParams = {
                    @MethodParam(name = "a", value = "a-value"),
                    @MethodParam(name = "b", value = "b-value"),
                    @MethodParam(name = "code", value = "test-periodical-scheduler")
            },
            period = @Period(period = 5, startDate = "01/01/2000 00:00:00")
    )
    @ScheduledBeanMethod(
            code = "test-fixed-delay-scheduler",
            isSingleton = false,
            fixedDelay = @FixedDelay(period = 5, startDate = "01/01/2000 00:00:00"),
            methodParams = {
                    @MethodParam(name = "a", value = "a-value-1"),
                    @MethodParam(name = "b", value = "b-value-1"),
                    @MethodParam(name = "code", value = "test-fixed-delay-scheduler")
            }
    )
    @ScheduledBeanMethod(
            code = "test-cron-scheduler",
            isSingleton = false,
            cron = @Cron(expression = "1-59/2 * * * *"),
            methodParams = {
                @MethodParam(name = "a", value = "a-value-2"),
                @MethodParam(name = "b", value = "b-value-2"),
                @MethodParam(name = "code", value = "test-cron-scheduler")
            }
    )
    public void testCorrectScheduledTasks(String a, String b, String code) {
        log.debug("Scheduler code: {} Test method has been executed with the following parameters: {}, {}", a, b);
    }

    @Override
    @ScheduledBeanMethod(
            code = "exception-when-number-of-parameters-dont-match",
            isSingleton = false,
            period = @Period(period = 5, startDate = "01/01/2000 00:00:00")
    )
    @ScheduledBeanMethod(
            code = "exception-when-two-scheduling-types-defined",
            isSingleton = false,
            period = @Period(period = 5, startDate = "01/01/2000 00:00:00"),
            cron = @Cron(expression = "1-59/2 * * * *"),
            methodParams = {
                    @MethodParam(name = "arg", value = "arg-value"),
            }
    )
    @ScheduledBeanMethod(
            code = "already-exists",
            isSingleton = false,
            period = @Period(period = 5, startDate = "01/01/2000 00:00:00"),
            methodParams = {
                @MethodParam(name = "arg", value = "arg-value"),
            }
    )
    public void testFailedScheduledTasks(String arg) {
        log.debug("This scheduled task should have not been created. If you see this message, then test didn't work");
    }
}
