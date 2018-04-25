-- begin SCHEDULER_SCHEDULER_LOADER_LOG
create table SCHEDULER_SCHEDULER_LOADER_LOG (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CODE varchar(255) not null,
    SCHEDULER_VERSION integer not null,
    SCHEDULED_TASK_ID varchar(36) not null,
    --
    primary key (ID)
)^
-- end SCHEDULER_SCHEDULER_LOADER_LOG
