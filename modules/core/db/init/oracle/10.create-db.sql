-- begin SCHEDULER_SCHEDULER_LOADER_LOG
create table SCHEDULER_SCHEDULER_LOADER_LOG (
    ID varchar2(32),
    VERSION number(10) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar2(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar2(50),
    DELETE_TS timestamp,
    DELETED_BY varchar2(50),
    --
    CODE varchar2(255) not null,
    SCHEDULED_TASK_ID varchar2(32) not null,
    --
    primary key (ID)
)^
-- end SCHEDULER_SCHEDULER_LOADER_LOG
