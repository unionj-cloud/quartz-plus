# Quartz Plus

[中文文档](README_zh.md) | [English Documentation](README.md)

A library to enhance Quartz scheduler with support for multiple independent `SchedulerFactoryBean` instances, enabling multiple independent job queues.

## Project Introduction

Quartz Plus is an enhancement to the Quartz scheduler in Spring Boot, allowing you to create multiple independent scheduler instances in the same application. Each scheduler has its own thread pool and database tables, operating independently without interference. It is particularly suitable for scenarios requiring isolation of different business contexts for scheduled tasks.

## Technology Stack

- Spring Boot 2.7.18
- Spring Boot Starter Quartz
- Quartz Scheduler
- JDBC JobStore (supports cluster mode)
- Java 1.8+

## Maven Coordinates

```xml
<dependency>
    <groupId>io.github.unionj-cloud</groupId>
    <artifactId>quartz-plus-core</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Project Structure

```
quartz-plus/
├── quartz-plus-core/          # Core module
│   ├── dto/                   # Data transfer objects
│   │   ├── JobRequest.java    # Job request object
│   │   └── GroupRequest.java  # Job group request object
│   ├── quartz/                # Quartz utility classes
│   │   └── QuartzUtils.java   # Quartz utility class, manages multiple schedulers
│   ├── service/               # Service layer
│   │   ├── QuartzPlusBaseService.java           # Job management service interface
│   │   └── impl/
│   │       └── DefaultQuartzPlusBaseServiceImpl.java  # Default implementation
│   ├── spring/                # Spring integration
│   │   ├── QuartzPlusJob.java              # Job identification annotation
│   │   ├── QuartzPlusJobScan.java          # Package scanning annotation
│   │   ├── QuartzPlusJobFactoryBean.java   # Scheduler factory Bean
│   │   ├── AutowiringSpringBeanJobFactory.java  # Auto-wiring Job factory
│   │   └── SchedulerConfig.java            # Scheduler configuration class
│   └── QuartzPlusCoreAutoConfiguration.java  # Auto-configuration class
└── quartz-plus-boot/          # Example module (for demonstration only)
```

## Core Module Description

### QuartzPlusCoreAutoConfiguration

Auto-configuration class that automatically registers the `QuartzPlusBaseService` Bean.

### QuartzPlusBaseService

Job management service interface that provides the following functions:

- `addScheduleJob(JobRequest jobRequest)` - Add scheduled job
- `updateScheduleJob(JobRequest jobRequest)` - Update scheduled job
- `deleteScheduleJob(JobRequest jobRequest)` - Delete scheduled job
- `pauseScheduleJob(JobRequest jobRequest)` - Pause scheduled job
- `resumeScheduleJob(JobRequest jobRequest)` - Resume scheduled job
- `immediatelyJob(JobRequest jobRequest)` - Execute job immediately
- `isJobRunning(JobRequest jobRequest)` - Check if job is running
- `isJobExists(JobRequest jobRequest)` - Check if job exists
- `getScheduleState(JobRequest jobRequest)` - Get job schedule state
- `pauseGroup(GroupRequest groupRequest)` - Pause job group
- `resumeGroup(GroupRequest groupRequest)` - Resume job group
- `stopRunningJob(JobRequest jobRequest)` - Stop running job

### JobRequest

Job request object containing the following attributes:

- `schedName` - Scheduler name (automatically set by the system)
- `jobGroup` - Job group name (default: DEV)
- `jobName` - Job name
- `jobClass` - Fully qualified job class name
- `cronExpression` - Cron expression (for CronTrigger)
- `startDateAt` - Start time (for SimpleTrigger)
- `repeatIntervalInSeconds` - Repeat interval in seconds (for SimpleTrigger)
- `repeatCount` - Repeat count (for SimpleTrigger)
- `jobDataMap` - Job data map
- `retry` - Retry identifier (default: N)
- `desc` - Description
- `reason` - Reason description

### GroupRequest

Job group request object containing the following attributes:

- `jobGroup` - Job group name (default: DEV)
- `jobClass` - Fully qualified job class name

### QuartzUtils

Utility class responsible for managing multiple scheduler instances:

- `addSchedulerFactoryBean(String jobBeanClassName, SchedulerFactoryBean schedulerFactoryBean)` - Register scheduler
- `getScheduler(JobRequest jobRequest)` - Get corresponding scheduler based on job request
- `getScheduler(GroupRequest groupRequest)` - Get corresponding scheduler based on group request
- `createJob(JobRequest jobRequest, Class<? extends Job> jobClass, ApplicationContext context)` - Create JobDetail
- `createTrigger(JobRequest jobRequest)` - Create Trigger (supports CronTrigger and SimpleTrigger)

### @QuartzPlusJob

Identifies a class as a Quartz Plus job. This annotation can specify the scheduler name (via the value attribute). If not specified, the class name converted to kebab-case is used.

### @QuartzPlusJobScan

Package scanning annotation used to scan classes annotated with `@QuartzPlusJob` in the specified package. Usage is similar to Spring's `@ComponentScan`:

```java
@QuartzPlusJobScan("com.example.job")
public class Application {
    // ...
}
```

### QuartzPlusJobFactoryBean

Factory Bean that creates an independent `SchedulerFactoryBean` for each job class annotated with `@QuartzPlusJob`. Creates schedulers based on configuration and supports the following configuration items:

- Scheduler instance name
- Thread pool thread count
- Thread name prefix
- Database table prefix
- Cluster mode
- Auto startup

### SchedulerConfig

Scheduler configuration class containing the following configuration items:

- `enabled` - Whether to enable this scheduler (default: false)
- `instanceName` - Scheduler instance name
- `threadCount` - Thread pool thread count (default: 3)
- `threadNamePrefix` - Thread name prefix
- `tablePrefix` - Database table prefix
- `clustered` - Whether to enable cluster mode (default: false)
- `description` - Scheduler description
- `autoStartup` - Whether to auto startup (default: false)

## Integration and Usage Instructions

### 1. Add Dependencies

Add dependencies in `pom.xml`:

```xml
<dependency>
    <groupId>io.github.unionj-cloud</groupId>
    <artifactId>quartz-plus-core</artifactId>
    <version>0.0.1</version>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
```

### 2. Configure Data Source

Configure data source and basic Quartz configuration in `application.properties` or `application.yml`:

```properties
# Data source configuration
spring.datasource.url=jdbc:mysql://localhost:3306/quartz-plus?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Quartz basic configuration
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=always # Automatically create required tables
spring.quartz.properties.org.quartz.jobStore.class=org.springframework.scheduling.quartz.LocalDataSourceJobStore
spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
spring.quartz.properties.org.quartz.jobStore.useProperties=false
spring.quartz.properties.org.quartz.jobStore.misfireThreshold=5000
spring.quartz.properties.org.quartz.jobStore.clusterCheckinInterval=15000
```

### 3. Create Job Class

Create a job class that extends `QuartzJobBean` and annotate it with `@QuartzPlusJob`:

```java
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import quartzplus.core.spring.QuartzPlusJob;

@Slf4j
@QuartzPlusJob("simpleJobA")
public class TestSimpleJobA extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("Executing TestSimpleJobA");
    }
}
```

The `value` attribute of the `@QuartzPlusJob` annotation specifies the configuration prefix for the scheduler, corresponding to the `quartz.scheduler.{value}.*` configuration items in the configuration file.

### 4. Configure Scheduler

Configure the corresponding scheduler for each job class in the configuration file:

```properties
# Configure scheduler named simple-job-a (@QuartzPlusJob value will be converted to kebab-case)
quartz.scheduler.simple-job-a.enabled=true
quartz.scheduler.simple-job-a.instance-name=SimpleJobAScheduler
quartz.scheduler.simple-job-a.thread-count=8
quartz.scheduler.simple-job-a.thread-name-prefix=SimpleJobA_
quartz.scheduler.simple-job-a.table-prefix=QRTZ_
quartz.scheduler.simple-job-a.clustered=true
quartz.scheduler.simple-job-a.description=simple job a
quartz.scheduler.simple-job-a.auto-startup=true
```

Configuration description:

- `enabled` - Whether to enable this scheduler (must be true, otherwise the scheduler will not be created)
- `instance-name` - Scheduler instance name. If not configured, the class name + "Scheduler" is used
- `thread-count` - Thread pool thread count, default is 3
- `thread-name-prefix` - Thread name prefix. If not configured, the scheduler name + "_" is used
- `table-prefix` - Database table prefix. If not configured, "QRTZ_" + scheduler name (removing "Scheduler") + "\_" is used. Data from different job queues will be persisted to different database tables, and these database tables need to be created manually. It is recommended to configure it as "QRTZ_" to directly use the tables automatically created by the framework
- `clustered` - Whether to enable cluster mode, default is false
- `description` - Scheduler description
- `auto-startup` - Whether to auto startup, default is false

### 5. Enable Package Scanning

Use the `@QuartzPlusJobScan` annotation on the startup class or configuration class to specify the scanning path for job classes:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import quartzplus.core.spring.QuartzPlusJobScan;

@SpringBootApplication
@QuartzPlusJobScan("com.example.job")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 6. Use Service to Manage Jobs

Inject `QuartzPlusBaseService` to manage jobs:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import quartzplus.core.dto.JobRequest;
import quartzplus.core.service.QuartzPlusBaseService;

import java.util.UUID;

@Configuration
public class AppConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private QuartzPlusBaseService quartzPlusBaseService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Create job request
        JobRequest jobRequest = new JobRequest();
        jobRequest.setJobName("testSimpleJobA" + UUID.randomUUID());
        jobRequest.setJobClass("com.example.job.TestSimpleJobA");
        jobRequest.setJobGroup("DEV");
        jobRequest.setCronExpression("0/10 * * * * ?");  // Execute every 10 seconds
        jobRequest.setDesc("Test job");
        
        // Add scheduled job
        quartzPlusBaseService.addScheduleJob(jobRequest);
    }
}
```

### 7. Job Types

Two job trigger types are supported:

#### Cron Expression Trigger

Use the `cronExpression` attribute:

```java
JobRequest jobRequest = new JobRequest();
jobRequest.setJobName("cronJob");
jobRequest.setJobClass("com.example.job.MyJob");
jobRequest.setCronExpression("0 0 12 * * ?");  // Execute at 12:00 every day
quartzPlusBaseService.addScheduleJob(jobRequest);
```

#### Simple Trigger

Use the `startDateAt`, `repeatIntervalInSeconds`, `repeatCount` attributes:

```java
JobRequest jobRequest = new JobRequest();
jobRequest.setJobName("simpleJob");
jobRequest.setJobClass("com.example.job.MyJob");
jobRequest.setStartDateAt(LocalDateTime.now().plusMinutes(1));
jobRequest.setRepeatIntervalInSeconds(60);  // Execute every 60 seconds
jobRequest.setRepeatCount(10);  // Execute 10 times
quartzPlusBaseService.addScheduleJob(jobRequest);
```

## Notes

1. **Job classes must implement `QuartzJobBean`**: All job classes must extend `org.springframework.scheduling.quartz.QuartzJobBean`.

2. **Must configure `enabled=true`**: If the `enabled` configuration of a scheduler is `false` or not configured, that scheduler will not be created.

3. **Job class name and configuration mapping**: The value of the `@QuartzPlusJob` annotation will be converted to kebab-case, corresponding to the `quartz.scheduler.{kebab-case}.*` configuration items in the configuration file. If the annotation value is empty, the class name converted to kebab-case is used.

4. **Database table prefix**: Each scheduler uses an independent table prefix to ensure data isolation between different schedulers.

5. **Cluster mode**: If cluster mode is required, configure `clustered=true` and ensure multiple application instances connect to the same database.

6. **Fully qualified job class name**: When using `QuartzPlusBaseService` to add jobs, `jobClass` must use the fully qualified name of the job class (e.g., `com.example.job.MyJob`).

## Example Code

For complete example code, please refer to the `quartz-plus-boot` module, which demonstrates how to use the `quartz-plus-core` module.
