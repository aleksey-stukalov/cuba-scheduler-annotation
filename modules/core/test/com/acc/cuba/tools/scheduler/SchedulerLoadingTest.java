package com.acc.cuba.tools.scheduler;

import ch.qos.logback.classic.LoggerContext;
import com.acc.cuba.tools.scheduler.entity.SchedulerLoaderLog;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.TypedQuery;
import com.haulmont.cuba.core.app.scheduled.MethodParameterInfo;
import com.haulmont.cuba.core.entity.ScheduledTask;
import com.haulmont.cuba.core.entity.SchedulingType;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.core.global.ViewRepository;
import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.testsupport.TestAppender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Aleksey Stukalov on 27/09/2017.
 */
public class SchedulerLoadingTest {

    @ClassRule
    public static SchedulerTestContainer container = SchedulerTestContainer.Common.INSTANCE;

    private static TestAppender appender = new TestAppender();

    @Before
    public void initLogger() {
        appender.start();
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger("com.acc.cuba.tools.scheduler.core.ScheduledTaskLoader").addAppender(appender);

        AppContext.Internals.startContext();
    }

    @Test
    public void testSuccessfullyCreatedScheduledTasks() {
        ViewRepository viewRepository = AppBeans.get(ViewRepository.class);
        try (Transaction tx = container.persistence().getTransaction()) {
            EntityManager em = container.entityManager();
            TypedQuery query =
                    em.createQuery("select e from scheduler$SchedulerLoaderLog e", SchedulerLoaderLog.class)
                        .setView(new View(SchedulerLoaderLog.class)
                                .addProperty("code")
                                .addProperty("scheduledTask", viewRepository.getView(ScheduledTask.class, View.LOCAL))
                        );
            List<SchedulerLoaderLog> schedulerLoaderLogs = query.getResultList();
            Map<String, ScheduledTask> schedulersMap = schedulerLoaderLogs.stream()
                    .collect(Collectors.toMap(SchedulerLoaderLog::getCode, SchedulerLoaderLog::getScheduledTask));

            ScheduledTask periodicalScheduler = schedulersMap.get("test-periodical-scheduler");
            ScheduledTask fixedDelayScheduler = schedulersMap.get("test-fixed-delay-scheduler");
            ScheduledTask cronScheduler = schedulersMap.get("test-cron-scheduler");

            //periodicalScheduler
            Assert.assertNotNull(periodicalScheduler);

            Assert.assertEquals("scheduler_TestScheduler", periodicalScheduler.getBeanName());
            Assert.assertEquals("testCorrectScheduledTasks", periodicalScheduler.getMethodName());
            Assert.assertEquals("admin", periodicalScheduler.getUserName());
            Assert.assertEquals(true, periodicalScheduler.getSingleton());
            Assert.assertEquals(true, periodicalScheduler.getActive() );
            Assert.assertEquals(true, periodicalScheduler.getLogStart());
            Assert.assertEquals(true, periodicalScheduler.getLogFinish());
            Assert.assertEquals(5, periodicalScheduler.getTimeout().intValue());
            Assert.assertEquals(10, periodicalScheduler.getTimeFrame().intValue());
            Assert.assertEquals("Test periodical scheduler", periodicalScheduler.getDescription());
            Assert.assertEquals("localhost:8080/scheduler-core", periodicalScheduler.getPermittedServers());
            Assert.assertEquals(SchedulingType.PERIOD, periodicalScheduler.getSchedulingType());
            Assert.assertEquals(5, periodicalScheduler.getPeriod().intValue());
            try {
                Date startDate = (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).parse("01.01.2000 00:00:00");
                Assert.assertEquals(startDate, periodicalScheduler.getStartDate());
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            testParams(periodicalScheduler.getMethodParameters(),
                    new MethodParameterInfo[] {
                            new MethodParameterInfo(String.class.getName(), "a", "a-value"),
                            new MethodParameterInfo(String.class.getName(), "b", "b-value"),
                            new MethodParameterInfo(String.class.getName(), "code", "test-periodical-scheduler")});

            //fixedDelayScheduler
            Assert.assertNotNull(fixedDelayScheduler);
            Assert.assertEquals("scheduler_TestScheduler", fixedDelayScheduler.getBeanName());
            Assert.assertEquals("testCorrectScheduledTasks", fixedDelayScheduler.getMethodName());
            Assert.assertEquals(false, fixedDelayScheduler.getSingleton());
            Assert.assertEquals(SchedulingType.FIXED_DELAY, fixedDelayScheduler.getSchedulingType());
            Assert.assertEquals(5, fixedDelayScheduler.getPeriod().intValue());
            try {
                Date startDate = (new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")).parse("01.01.2000 00:00:00");
                Assert.assertEquals(startDate, fixedDelayScheduler.getStartDate());
            } catch (ParseException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            testParams(fixedDelayScheduler.getMethodParameters(),
                    new MethodParameterInfo[] {
                            new MethodParameterInfo(String.class.getName(), "a", "a-value-1"),
                            new MethodParameterInfo(String.class.getName(), "b", "b-value-1"),
                            new MethodParameterInfo(String.class.getName(), "code", "test-fixed-delay-scheduler")});

            //cronScheduler
            Assert.assertNotNull(cronScheduler);
            Assert.assertEquals("scheduler_TestScheduler", cronScheduler.getBeanName());
            Assert.assertEquals("testCorrectScheduledTasks", cronScheduler.getMethodName());
            Assert.assertEquals(false, cronScheduler.getSingleton());
            Assert.assertEquals(SchedulingType.CRON, cronScheduler.getSchedulingType());
            Assert.assertEquals("1-59/2 * * * *", cronScheduler.getCron());

            testParams(cronScheduler.getMethodParameters(),
                    new MethodParameterInfo[] {
                            new MethodParameterInfo(String.class.getName(), "a", "a-value-2"),
                            new MethodParameterInfo(String.class.getName(), "b", "b-value-2"),
                            new MethodParameterInfo(String.class.getName(), "code", "test-cron-scheduler")});

            tx.commit();
        }
    }

    @Test
    public void testFailedScheduledTasks() {
        Assert.assertEquals(1, appender.filterMessages(m -> m.equals("Scheduler [exception-when-number-of-parameters-dont-match] definition doesn't comply the actual parameters number: bean: scheduler_TestScheduler, method: testFailedScheduledTasks")).count());
        Assert.assertEquals(1, appender.filterMessages(m -> m.equals("Scheduler [exception-when-two-scheduling-types-defined] has more than one Scheduling Type defined for : bean: scheduler_TestScheduler, method: testFailedScheduledTasks")).count());
        Assert.assertEquals(1, appender.filterMessages(m -> m.equals("Scheduler [already-exists] has already been imported for bean: scheduler_TestScheduler, method: testFailedScheduledTasks")).count());
    }

    private void testParams(List<MethodParameterInfo> parameters, MethodParameterInfo[] expectedValues) {
        int paramsCount = parameters.size();
        int valuesCount = expectedValues.length;

        Assert.assertEquals(paramsCount, valuesCount);

        for (int i = 0; i < paramsCount; i++) {
            MethodParameterInfo param = parameters.get(i);
            MethodParameterInfo expectedValue = expectedValues[i];

            Assert.assertEquals(expectedValue.getName(), param.getName());
            Assert.assertEquals(expectedValue.getType(), param.getType());
            Assert.assertEquals(expectedValue.getValue(), param.getValue());
        }
    }
}
