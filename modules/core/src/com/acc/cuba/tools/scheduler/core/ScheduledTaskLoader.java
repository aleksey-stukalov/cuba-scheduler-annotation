package com.acc.cuba.tools.scheduler.core;

import com.acc.cuba.tools.scheduler.core.annotation.MethodParam;
import com.acc.cuba.tools.scheduler.core.annotation.ScheduledBeanMethod;
import com.acc.cuba.tools.scheduler.core.annotation.ScheduledBeanMethods;
import com.acc.cuba.tools.scheduler.core.exception.WrongSchedulerDefinitionException;
import com.acc.cuba.tools.scheduler.entity.SchedulerLoaderLog;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.app.scheduled.MethodParameterInfo;
import com.haulmont.cuba.core.entity.ScheduledTask;
import com.haulmont.cuba.core.entity.ScheduledTaskDefinedBy;
import com.haulmont.cuba.core.entity.SchedulingType;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by aleksey on 21/09/2017.
 */
@Component
public class ScheduledTaskLoader implements BeanPostProcessor {

    @Inject
    private Metadata metadata;

    @Inject
    private Persistence persistence;

    @Inject
    private DataManager dataManager;

    @Inject
    private Logger log;

    private List<ScheduledMethodContext> scheduleAnnotatedMethods = new ArrayList<>();

    boolean isScheduledTaskLoaded(String code) {
        SchedulerLoaderLog loaderLog =
            dataManager.load(LoadContext.create(SchedulerLoaderLog.class)
                    .setQuery(LoadContext.createQuery("select e from scheduler$SchedulerLoaderLog e where e.code = :code")
                            .setParameter("code", code)));

        return loaderLog != null;
    }

    public void loadScheduledMethods() {
        for (ScheduledMethodContext smc : scheduleAnnotatedMethods) {
            for (ScheduledBeanMethod scheduleConfig : smc.getScheduledBeanMethods()) {
                loadTaskForScheduledBeanMethod(smc.getBeanName(), smc.getMethod(), scheduleConfig);
            }
        }
    }

    public boolean loadTaskForScheduledBeanMethod(String beanName, Method method, ScheduledBeanMethod config) {
        if (isScheduledTaskLoaded(config.code())) {
            log.info("Scheduler has already been imported for bean: {}, method: {}", beanName, method.getName());
            return false;
        }

        try (Transaction tx = persistence.createTransaction()) {
            ScheduledTask scheduledTask = metadata.create(ScheduledTask.class);

            scheduledTask.setBeanName(beanName);
            scheduledTask.setMethodName(method.getName());
            scheduledTask.setDefinedBy(ScheduledTaskDefinedBy.BEAN);
            scheduledTask.setActive(config.isActive());
            scheduledTask.setUserName(StringUtils.trimToNull(config.userName()));
            scheduledTask.setSingleton(config.isSingleton());
            scheduledTask.setTimeout(config.timeout() > 0 ? config.timeout() : null);
            scheduledTask.setTimeFrame(config.timeFrame() > 0 ? config.timeFrame() : null);
            scheduledTask.setPermittedServers(StringUtils.trimToNull(config.permittedServers()));
            scheduledTask.setLogStart(config.logStart());
            scheduledTask.setLogFinish(config.logFinish());
            scheduledTask.setDescription(StringUtils.trimToNull(config.description()));

            //set parameters
            if (method.getParameters().length != config.methodParams().length)
                throw new WrongSchedulerDefinitionException(String.format("@MethodParam[] definition doesn't comply the actual number parameters of the method for bean: %s, method: %s", beanName, method.getName()));

            List<Class> paramsTypes = Arrays.stream(method.getParameters()).map(Parameter::getType).collect(Collectors.toList());

            // assumed that MethodParam-s are following in the same order as parameters of the method to map types to them
            List<MethodParameterInfo> params = new ArrayList<>();
            for (MethodParam param : config.methodParams()) {
                params.add(new MethodParameterInfo(paramsTypes.iterator().next().getName(), param.name(), param.value()));
            }
            scheduledTask.updateMethodParameters(params);

            SchedulingType type = getSchedulingType(beanName, method, config);

            scheduledTask.setSchedulingType(type);
            switch (type) {
                case CRON:
                    scheduledTask.setCron(config.cron().expression());
                    break;

                case PERIOD:
                    //pass through

                case FIXED_DELAY:
                    scheduledTask.setPeriod(config.period().period());
                    try {
                        Date startDate = (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).parse(config.period().startDate());
                        scheduledTask.setStartDate(startDate);
                    } catch (ParseException e) {
                        throw new RuntimeException(String.format("Start date was not parsed for bean: %s, method: %s", beanName, method.getName()), e);
                    }
                    break;
            }

            SchedulerLoaderLog loaderLog = metadata.create(SchedulerLoaderLog.class);
            loaderLog.setCode(config.code());
            loaderLog.setScheduledTask(scheduledTask);

            persistence.getEntityManager().persist(scheduledTask);
            persistence.getEntityManager().persist(loaderLog);
            log.info("Scheduler has been successfully imported for bean: {}, method: {}", beanName, method.getName());
            tx.commit();
            return true;
        }
    }

    private SchedulingType getSchedulingType(String beanName, Method method, ScheduledBeanMethod config) throws WrongSchedulerDefinitionException {
        Set<SchedulingType> definedTypes = new HashSet<>();

        if (!config.period().startDate().equals(""))
            definedTypes.add(SchedulingType.PERIOD);

        if (!config.cron().expression().equals(""))
            definedTypes.add(SchedulingType.CRON);

        if (!config.fixedDelay().startDate().equals(""))
            definedTypes.add(SchedulingType.FIXED_DELAY);

        if (definedTypes.size() == 1)
            return definedTypes.iterator().next();
        else
            throw new WrongSchedulerDefinitionException(
                    String.format("Scheduler has more than one Scheduling Type defined for : bean: %s, method: %s",
                            beanName, method.getName()));
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /** The method scans all beans, that contain methods, annotated as {@link ScheduledBeanMethod}
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        Map<Method, Set<ScheduledBeanMethod>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<Set<ScheduledBeanMethod>>) method -> {
                    Set<ScheduledBeanMethod> scheduledMethods = AnnotatedElementUtils.getMergedRepeatableAnnotations(
                            method, ScheduledBeanMethod.class, ScheduledBeanMethods.class);
                    return (!scheduledMethods.isEmpty() ? scheduledMethods : null);
                });

        for (Map.Entry<Method, Set<ScheduledBeanMethod>> entry : annotatedMethods.entrySet()) {
            scheduleAnnotatedMethods.add(new ScheduledMethodContext(beanName, entry.getKey(), entry.getValue()));
        }

        return bean;
    }
}
