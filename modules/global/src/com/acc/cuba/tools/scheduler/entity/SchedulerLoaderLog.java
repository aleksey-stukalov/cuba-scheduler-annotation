package com.acc.cuba.tools.scheduler.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.haulmont.cuba.core.entity.ScheduledTask;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;
import com.haulmont.cuba.security.entity.User;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.chile.core.annotations.NamePattern;
import javax.validation.constraints.NotNull;
import javax.persistence.Index;

@NamePattern("%s|code")
@Table(name = "SCHEDULER_SCHEDULER_LOADER_LOG", indexes = {
    @Index(name = "IDX_SCHEDULER_SCHEDULER_LOADER_LOG", columnList = "SCHEDULED_TASK_ID, VERSION")
})
@Entity(name = "scheduler$SchedulerLoaderLog")
public class SchedulerLoaderLog extends StandardEntity {
    private static final long serialVersionUID = -2242981040158529077L;

    @Column(name = "CODE", nullable = false, unique = true)
    protected String code;

    @NotNull
    @Column(name = "SCHEDULER_VERSION", nullable = false)
    protected Integer schedulerVersion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SCHEDULED_TASK_ID", unique = true)
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @OnDelete(DeletePolicy.CASCADE)
    protected ScheduledTask scheduledTask;

    public void setSchedulerVersion(Integer schedulerVersion) {
        this.schedulerVersion = schedulerVersion;
    }

    public Integer getSchedulerVersion() {
        return schedulerVersion;
    }


    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setScheduledTask(ScheduledTask scheduledTask) {
        this.scheduledTask = scheduledTask;
    }

    public ScheduledTask getScheduledTask() {
        return scheduledTask;
    }

}