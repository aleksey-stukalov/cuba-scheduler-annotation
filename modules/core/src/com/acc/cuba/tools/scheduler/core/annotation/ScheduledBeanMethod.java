package com.acc.cuba.tools.scheduler.core.annotation;

import java.lang.annotation.*;

/**
 * Created by Aleksey Stukalov on 21/09/2017.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ScheduledBeanMethods.class)
@Documented
public @interface ScheduledBeanMethod {

    /**
    To identify if scheduler is loaded
     */
    String code();

    /**
     If version if 0 update will never happen once it is initially loaded
     */
    int version() default 0;

    MethodParam[] methodParams() default {};

    String userName() default "";

    boolean isSingleton();

    boolean isActive() default false;

    int timeout() default -1;

    int timeFrame() default -1;

    String permittedServers() default "";

    boolean logStart() default false;

    boolean logFinish() default false;

    String description() default "";

    Period period() default @Period(period = -1, startDate = "");

    Cron cron() default @Cron(expression = "");

    FixedDelay fixedDelay() default @FixedDelay(period = -1, startDate = "");
}
