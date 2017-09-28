package com.acc.cuba.tools.scheduler.core;

import com.acc.cuba.tools.scheduler.core.annotation.ScheduledBeanMethod;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created by aleksey on 27/09/2017.
 */
public class ScheduledMethodContext {

    private  Set<ScheduledBeanMethod> scheduledBeanMethods;

    private String beanName;

    private Method method;

    public ScheduledMethodContext(String beanName, Method method, Set<ScheduledBeanMethod> scheduledBeanMethods) {
        this.scheduledBeanMethods = scheduledBeanMethods;
        this.beanName = beanName;
        this.method = method;
    }

    public Set<ScheduledBeanMethod> getScheduledBeanMethods() {
        return scheduledBeanMethods;
    }

    public void setScheduledBeanMethods(Set<ScheduledBeanMethod> scheduledBeanMethods) {
        this.scheduledBeanMethods = scheduledBeanMethods;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
