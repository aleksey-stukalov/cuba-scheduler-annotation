package com.acc.cuba.tools.scheduler.core.annotation;

import java.lang.annotation.*;

/**
 * Created by aleksey on 27/09/2017.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ScheduledBeanMethods {
    ScheduledBeanMethod[] value();
}
