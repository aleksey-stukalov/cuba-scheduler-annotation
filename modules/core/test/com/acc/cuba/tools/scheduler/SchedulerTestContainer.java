package com.acc.cuba.tools.scheduler;

import com.haulmont.cuba.testsupport.TestContainer;

import java.util.Arrays;

/**
 * Created by Aleksey Stukalov on 27/09/2017.
 */
public class SchedulerTestContainer extends TestContainer {

    public SchedulerTestContainer() {
        super();
        appComponents = Arrays.asList("com.haulmont.cuba");
        appPropertiesFiles = Arrays.asList("com/acc/cuba/tools/scheduler/app.properties",
                "com/acc/cuba/tools/scheduler/test-app.properties");

        dbDriver = "org.hsqldb.jdbc.JDBCDriver";
        dbUrl = "jdbc:hsqldb:hsql://localhost/scheduler";
        dbUser = "sa";
        dbPassword = "";
    }

    public static class Common extends SchedulerTestContainer {

        public static final SchedulerTestContainer.Common INSTANCE = new SchedulerTestContainer.Common();

        private static volatile boolean initialized;

        private Common() {
        }

        @Override
        public void before() throws Throwable {
            if (!initialized) {
                super.before();
                initialized = true;
            }
            setupContext();
        }

        @Override
        public void after() {
            cleanupContext();
            // never stops - do not call super
        }
    }


}
