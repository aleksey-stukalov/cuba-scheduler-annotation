package com.acc.cuba.tools.scheduler.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Aleksey Stukalov on 21/09/2017.
 */

@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FixedDelay {
    /**
     * @return task execution interval or delay in seconds if Scheduling type is Period or Fixed Delay
     */
    int period();
    /**
     * Start date to be set in dd/MM/yyyy HH:mm:ss format
     * @return the date/time of the first launch
     */
    String startDate();
}
