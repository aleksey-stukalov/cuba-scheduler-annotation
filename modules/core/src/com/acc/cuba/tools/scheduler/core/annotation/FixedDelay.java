package com.acc.cuba.tools.scheduler.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by aleksey on 21/09/2017.
 */

@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FixedDelay {
    int period();
    String startDate();
}
