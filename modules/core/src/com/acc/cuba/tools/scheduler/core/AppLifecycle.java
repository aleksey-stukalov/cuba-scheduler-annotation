package com.acc.cuba.tools.scheduler.core;

import com.haulmont.cuba.core.sys.AppContext;
import com.haulmont.cuba.security.app.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Created by aleksey on 21/09/2017.
 */

@Component("scheduler_AppLifecycle")
public class AppLifecycle implements AppContext.Listener {

    @Inject
    private ScheduledTaskLoader taskLoader;

    @Inject
    private Authentication auth;

    @Override
    public void applicationStarted() {
        auth.withUser(null, () -> {
            taskLoader.loadScheduledMethods();
            return null;
        });
    }

    @Override
    public void applicationStopped() {
    }

    @PostConstruct
    public void postConstruct() {
        AppContext.addListener(this);
    }
}
