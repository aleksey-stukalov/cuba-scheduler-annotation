package com.acc.cuba.tools.scheduler.core;

import com.acc.cuba.tools.scheduler.core.annotation.MethodParam;
import com.acc.cuba.tools.scheduler.core.annotation.Period;
import com.acc.cuba.tools.scheduler.core.annotation.ScheduledBeanMethod;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.concurrent.Callable;

/**
 * Created by aleksey on 27/09/2017.
 */
@Component
public class TestScheduler implements Callable {

    @Inject
    private Logger log;

    @ScheduledBeanMethod(
            code = "test_scheduler",
            isSingleton = true,
            isActive = true,
            period = @Period(period = 5, startDate = "01.01.2000 00:00:00"),
            logStart = true,
            methodParams = {
                    @MethodParam(name = "a", value = "a-value"),
                    @MethodParam(name = "b", value = "b-value")
            }
    )
    public void testPeriodMethod(String a, String b) {
        log.debug("Test method has been executed with the following parameters: {}, {}", a, b);
    }

    @Override
    public Object call() throws Exception {
        return null;
    }
}
