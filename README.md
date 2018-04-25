# cuba-scheduler-annotation
This application component enables developers to create Scheduled Tasks right at design time using specific annotations.

## Scheduler Declaration using Annotations

The component introduces a few [annotations](https://github.com/aleksey-stukalov/cuba-scheduler-annotation/blob/master/modules/core/src/com/acc/cuba/tools/scheduler/core/annotation/ScheduledBeanMethod.java) that fully comply with the definition of [a bean type sheduled task](https://doc.cuba-platform.com/manual-6.6/scheduled_tasks_cuba_reg.html) in CUBA.

The annotations are being scanned while server startup, so the component create corresponding CUBA scheduled tasks. In order to prevent multi-creating of the same scheduler, after the annotations are processed and the corresponding tasks are created, this information is stored in [ShedulerLoaderLog](https://github.com/aleksey-stukalov/cuba-scheduler-annotation/blob/master/modules/global/src/com/acc/cuba/tools/scheduler/entity/SchedulerLoaderLog.java) to be checked at the following starts. 

## Sample Code

Find an example of scheduling a method of a component from the source code below:

```java
@Component
public class TestScheduler implements TestSchedulerInterface {

    @Inject
    private Logger log;

    @Override
    @ScheduledBeanMethod(
            code = "test_scheduler",
            isSingleton = true,
            isActive = true,
            period = @Period(period = 5, startDate = "01/01/2000 00:00:00"),
            logStart = true,
            methodParams = {
                    @MethodParam(name = "a", value = "a-value"),
                    @MethodParam(name = "b", value = "b-value")
            }
    )
    public void testPeriodMethod(String a, String b) {
        log.debug("test method has been executed with the following parameters: {}. {}", a, b);
    }
}
```

## How to Use

Add the ```https://dl.bintray.com/aleksey-stukalov/cuba-components``` repo to your ```build.gradle``` as it is shown here:
```
buildscript {
    ...
    repositories {
        
        ...
        maven {
            url  "https://dl.bintray.com/aleksey-stukalov/cuba-components"
        }
    }
    ...
}
```

Add the application component using the CUBA Studio:
- Artifact group: ```com.acc.cuba.tools.scheduler```
- Artifact name: ```scheduler-global```
- Version: _pick the compatible version from the table below_

| Platform Version | Add-on Version |
| ---------------- | -------------- |
| 6.8.4            | 1.0.0          |
| 6.8.6            | 1.0.1          |

Make sure you enabled scheduling by adding the ```cuba.schedulingActive = true``` line into the app.properties file in the core module.
