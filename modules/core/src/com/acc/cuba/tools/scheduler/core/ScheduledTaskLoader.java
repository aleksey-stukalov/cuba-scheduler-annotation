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
import com.haulmont.cuba.core.global.*;
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
 * Created by Aleksey Stukalov on 21/09/2017.
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

    protected SchedulerLoaderLog getLoadedScheduledTaskLog(String code) {
        return
            dataManager.load(LoadContext.create(SchedulerLoaderLog.class)
                    .setQuery(LoadContext.createQuery("select e from scheduler$SchedulerLoaderLog e where e.code = :code")
                            .setParameter("code", code)));
    }

    public void loadScheduledMethods() {
        for (ScheduledMethodContext smc : scheduleAnnotatedMethods) {
            for (ScheduledBeanMethod scheduleConfig : smc.getScheduledBeanMethods())
            try {
                loadTaskForScheduledBeanMethod(smc.getBeanName(), smc.getMethod(), scheduleConfig);
            } catch (WrongSchedulerDefinitionException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    protected boolean loadTaskForScheduledBeanMethod(String beanName, Method method, ScheduledBeanMethod config) {
        ScheduledTask scheduledTask = null;
        SchedulerLoaderLog loaderLog = getLoadedScheduledTaskLog(config.code());
        if (loaderLog != null) {

            if (loaderLog.getSchedulerVersion() == null
                || config.version() == 0
                || loaderLog.getSchedulerVersion() <= config.version()) {

                log.info(String.format("Scheduler [%s] has already been imported for bean: %s, method: %s", config.code(), beanName, method.getName()));
                return false;
            }

            scheduledTask = loaderLog.getScheduledTask();
        }


        try (Transaction tx = persistence.createTransaction()) {

            if (scheduledTask == null)
                scheduledTask = metadata.create(ScheduledTask.class);
            else
                scheduledTask = persistence.getEntityManager().merge(scheduledTask);

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
                throw new WrongSchedulerDefinitionException(String.format("Scheduler [%s] definition doesn't comply the actual parameters number: bean: %s, method: %s", config.code(), beanName, method.getName()));

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

                case FIXED_DELAY:
                    scheduledTask.setPeriod(config.fixedDelay().period());
                    try {
                        Date startDate = (new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")).parse(config.fixedDelay().startDate());
                        scheduledTask.setStartDate(startDate);
                    } catch (ParseException e) {
                        throw new WrongSchedulerDefinitionException(String.format("Start date was not parsed for scheduler: %s, bean: %s, method: %s", config.code(), beanName, method.getName()), e);
                    }
                    break;

                case PERIOD:
                    scheduledTask.setPeriod(config.period().period());
                    try {
                        Date startDate = (new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")).parse(config.period().startDate());
                        scheduledTask.setStartDate(startDate);
                    } catch (ParseException e) {
                        throw new WrongSchedulerDefinitionException(String.format("Start date was not parsed for scheduler: %s, bean: %s, method: %s", config.code(), beanName, method.getName()), e);
                    }
                    break;
            }

            if (loaderLog == null)
                loaderLog = metadata.create(SchedulerLoaderLog.class);
            else
                loaderLog = persistence.getEntityManager().merge(loaderLog);

            loaderLog.setCode(config.code());
            loaderLog.setVersion(config.version());
            loaderLog.setScheduledTask(scheduledTask);

            if (PersistenceHelper.isNew(scheduledTask))
                persistence.getEntityManager().persist(scheduledTask);

            if (PersistenceHelper.isNew(loaderLog))
                persistence.getEntityManager().persist(loaderLog);

            log.info("Scheduler [{}] has been successfully imported for bean: {}, method: {}", config.code(), beanName, method.getName());
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
                    String.format("Scheduler [%s] has more than one Scheduling Type defined for : bean: %s, method: %s",
                            config.code(), beanName, method.getName()));
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        return bean;
    }

    /**
     * The method scans all beans, that contain methods,
     * annotated as {@link ScheduledBeanMethod}
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
