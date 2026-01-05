# Quartz Plus

[中文文档](README_zh.md) | [English Documentation](README.md)

一个增强 Quartz 调度器的库，支持多个独立的 `SchedulerFactoryBean`，从而实现多个独立的作业队列。

## 项目简介

Quartz Plus 是对 Spring Boot 中 Quartz 调度器的增强，允许你在同一个应用中创建多个独立的调度器实例。每个调度器拥有独立的线程池和数据库表，互不干扰，非常适合需要隔离不同业务场景的定时任务场景。

## 技术栈

- Spring Boot 2.7.18
- Spring Boot Starter Quartz
- Quartz Scheduler
- JDBC JobStore（支持集群模式）
- Java 1.8+

## Maven 坐标

```xml
<dependency>
    <groupId>io.github.unionj-cloud</groupId>
    <artifactId>quartz-plus-core</artifactId>
    <version>0.0.1</version>
</dependency>
```

## 项目结构

```
quartz-plus/
├── quartz-plus-core/          # 核心模块
│   ├── dto/                   # 数据传输对象
│   │   ├── JobRequest.java    # 作业请求对象
│   │   └── GroupRequest.java  # 作业组请求对象
│   ├── quartz/                # Quartz 工具类
│   │   └── QuartzUtils.java   # Quartz 工具类，管理多个调度器
│   ├── service/               # 服务层
│   │   ├── QuartzPlusBaseService.java           # 作业管理服务接口
│   │   └── impl/
│   │       └── DefaultQuartzPlusBaseServiceImpl.java  # 默认实现
│   ├── spring/                # Spring 集成
│   │   ├── QuartzPlusJob.java              # 作业标识注解
│   │   ├── QuartzPlusJobScan.java          # 包扫描注解
│   │   ├── QuartzPlusJobFactoryBean.java   # 调度器工厂Bean
│   │   ├── AutowiringSpringBeanJobFactory.java  # 自动装配Job工厂
│   │   └── SchedulerConfig.java            # 调度器配置类
│   └── QuartzPlusCoreAutoConfiguration.java  # 自动配置类
└── quartz-plus-boot/          # 示例模块（仅用于演示）
```

## 核心模块说明

### QuartzPlusCoreAutoConfiguration

自动配置类，自动注册 `QuartzPlusBaseService` Bean。

### QuartzPlusBaseService

作业管理服务接口，提供以下功能：

- `addScheduleJob(JobRequest jobRequest)` - 添加定时作业
- `updateScheduleJob(JobRequest jobRequest)` - 更新定时作业
- `deleteScheduleJob(JobRequest jobRequest)` - 删除定时作业
- `pauseScheduleJob(JobRequest jobRequest)` - 暂停定时作业
- `resumeScheduleJob(JobRequest jobRequest)` - 恢复定时作业
- `immediatelyJob(JobRequest jobRequest)` - 立即执行作业
- `isJobRunning(JobRequest jobRequest)` - 检查作业是否正在运行
- `isJobExists(JobRequest jobRequest)` - 检查作业是否存在
- `getScheduleState(JobRequest jobRequest)` - 获取作业调度状态
- `pauseGroup(GroupRequest groupRequest)` - 暂停作业组
- `resumeGroup(GroupRequest groupRequest)` - 恢复作业组
- `stopRunningJob(JobRequest jobRequest)` - 停止正在运行的作业

### JobRequest

作业请求对象，包含以下属性：

- `schedName` - 调度器名称（由系统自动设置）
- `jobGroup` - 作业组名称（默认：DEV）
- `jobName` - 作业名称
- `jobClass` - 作业类全限定名
- `cronExpression` - Cron 表达式（用于 CronTrigger）
- `startDateAt` - 开始时间（用于 SimpleTrigger）
- `repeatIntervalInSeconds` - 重复间隔（秒，用于 SimpleTrigger）
- `repeatCount` - 重复次数（用于 SimpleTrigger）
- `jobDataMap` - 作业数据映射
- `retry` - 重试标识（默认：N）
- `desc` - 描述信息
- `reason` - 原因说明

### GroupRequest

作业组请求对象，包含以下属性：

- `jobGroup` - 作业组名称（默认：DEV）
- `jobClass` - 作业类全限定名

### QuartzUtils

工具类，负责管理多个调度器实例：

- `addSchedulerFactoryBean(String jobBeanClassName, SchedulerFactoryBean schedulerFactoryBean)` - 注册调度器
- `getScheduler(JobRequest jobRequest)` - 根据作业请求获取对应的调度器
- `getScheduler(GroupRequest groupRequest)` - 根据组请求获取对应的调度器
- `createJob(JobRequest jobRequest, Class<? extends Job> jobClass, ApplicationContext context)` - 创建 JobDetail
- `createTrigger(JobRequest jobRequest)` - 创建 Trigger（支持 CronTrigger 和 SimpleTrigger）

### @QuartzPlusJob

标识一个类为 Quartz Plus 作业。该注解可以指定调度器的名称（通过 value 属性），如果不指定，则使用类名转换为 kebab-case。

### @QuartzPlusJobScan

包扫描注解，用于扫描指定包下的 `@QuartzPlusJob` 注解的类。用法类似 Spring 的 `@ComponentScan`：

```java
@QuartzPlusJobScan("com.example.job")
public class Application {
    // ...
}
```

### QuartzPlusJobFactoryBean

工厂Bean，为每个 `@QuartzPlusJob` 注解的作业类创建一个独立的 `SchedulerFactoryBean`。根据配置创建调度器，支持以下配置项：

- 调度器实例名称
- 线程池线程数
- 线程名称前缀
- 数据库表前缀
- 集群模式
- 自动启动

### SchedulerConfig

调度器配置类，包含以下配置项：

- `enabled` - 是否启用该调度器（默认：false）
- `instanceName` - 调度器实例名称
- `threadCount` - 线程池线程数（默认：3）
- `threadNamePrefix` - 线程名称前缀
- `tablePrefix` - 数据库表前缀
- `clustered` - 是否开启集群模式（默认：false）
- `description` - 调度器描述
- `autoStartup` - 是否自动启动（默认：false）

## 集成和使用说明

### 1. 添加依赖

在 `pom.xml` 中添加依赖：

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

### 2. 配置数据源

在 `application.properties` 或 `application.yml` 中配置数据源和 Quartz 基本配置：

```properties
# 数据源配置
spring.datasource.url=jdbc:mysql://localhost:3306/quartz-plus?useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Quartz 基本配置
spring.quartz.job-store-type=jdbc
spring.quartz.jdbc.initialize-schema=always # 自动创建所需的表
spring.quartz.properties.org.quartz.jobStore.class=org.springframework.scheduling.quartz.LocalDataSourceJobStore
spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
spring.quartz.properties.org.quartz.jobStore.useProperties=false
spring.quartz.properties.org.quartz.jobStore.misfireThreshold=5000
spring.quartz.properties.org.quartz.jobStore.clusterCheckinInterval=15000
```

### 3. 创建作业类

创建继承自 `QuartzJobBean` 的作业类，并使用 `@QuartzPlusJob` 注解标识：

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
        log.info("执行 TestSimpleJobA");
    }
}
```

`@QuartzPlusJob` 注解的 value 属性指定了调度器的配置前缀，对应配置文件中的 `quartz.scheduler.{value}.*` 配置项。

### 4. 配置调度器

在配置文件中为每个作业类配置对应的调度器：

```properties
# 配置名为 simple-job-a 的调度器（@QuartzPlusJob 的 value 值会转换为 kebab-case）
quartz.scheduler.simple-job-a.enabled=true
quartz.scheduler.simple-job-a.instance-name=SimpleJobAScheduler
quartz.scheduler.simple-job-a.thread-count=8
quartz.scheduler.simple-job-a.thread-name-prefix=SimpleJobA_
quartz.scheduler.simple-job-a.table-prefix=QRTZ_
quartz.scheduler.simple-job-a.clustered=true
quartz.scheduler.simple-job-a.description=simple job a
quartz.scheduler.simple-job-a.auto-startup=true
```

配置说明：

- `enabled` - 是否启用该调度器（必须为 true，否则不会创建调度器）
- `instance-name` - 调度器实例名称，如果不配置，则使用类名 + "Scheduler"
- `thread-count` - 线程池线程数，默认为 3
- `thread-name-prefix` - 线程名称前缀，如果不配置，则使用调度器名称 + "_"
- `table-prefix` - 数据库表前缀，如果不配置，则使用 "QRTZ_" + 调度器名称（去掉"Scheduler"） + "\_"，不同作业队列的数据将持久化到不同的数据库表，这些数据库表需自行创建。建议配置为"QRTZ_"，可以直接使用框架自动创建的表
- `clustered` - 是否开启集群模式，默认为 false
- `description` - 调度器描述
- `auto-startup` - 是否自动启动，默认为 false

### 5. 启用包扫描

在启动类或配置类上使用 `@QuartzPlusJobScan` 注解指定作业类的扫描路径：

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

### 6. 使用服务管理作业

注入 `QuartzPlusBaseService` 来管理作业：

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
        // 创建作业请求
        JobRequest jobRequest = new JobRequest();
        jobRequest.setJobName("testSimpleJobA" + UUID.randomUUID());
        jobRequest.setJobClass("com.example.job.TestSimpleJobA");
        jobRequest.setJobGroup("DEV");
        jobRequest.setCronExpression("0/10 * * * * ?");  // 每10秒执行一次
        jobRequest.setDesc("测试作业");
        
        // 添加定时作业
        quartzPlusBaseService.addScheduleJob(jobRequest);
    }
}
```

### 7. 作业类型

支持两种作业触发方式：

#### Cron 表达式触发

使用 `cronExpression` 属性：

```java
JobRequest jobRequest = new JobRequest();
jobRequest.setJobName("cronJob");
jobRequest.setJobClass("com.example.job.MyJob");
jobRequest.setCronExpression("0 0 12 * * ?");  // 每天12点执行
quartzPlusBaseService.addScheduleJob(jobRequest);
```

#### Simple 触发

使用 `startDateAt`、`repeatIntervalInSeconds`、`repeatCount` 属性：

```java
JobRequest jobRequest = new JobRequest();
jobRequest.setJobName("simpleJob");
jobRequest.setJobClass("com.example.job.MyJob");
jobRequest.setStartDateAt(LocalDateTime.now().plusMinutes(1));
jobRequest.setRepeatIntervalInSeconds(60);  // 每60秒执行一次
jobRequest.setRepeatCount(10);  // 执行10次
quartzPlusBaseService.addScheduleJob(jobRequest);
```

## 注意事项

1. **作业类必须实现 `QuartzJobBean`**：所有作业类必须继承自 `org.springframework.scheduling.quartz.QuartzJobBean`。

2. **必须配置 `enabled=true`**：如果某个调度器的 `enabled` 配置为 `false` 或未配置，则该调度器不会被创建。

3. **作业类名与配置的映射**：`@QuartzPlusJob` 注解的 value 值会转换为 kebab-case，对应配置文件中的 `quartz.scheduler.{kebab-case}.*` 配置项。如果注解的 value 为空，则使用类名转换为 kebab-case。

4. **数据库表前缀**：每个调度器使用独立的表前缀，确保不同调度器的数据隔离。

5. **集群模式**：如果需要集群模式，需要配置 `clustered=true`，并确保多个应用实例连接到同一个数据库。

6. **作业类全限定名**：在使用 `QuartzPlusBaseService` 添加作业时，`jobClass` 必须使用作业类的全限定名（如：`com.example.job.MyJob`）。

## 示例代码

完整的示例代码请参考 `quartz-plus-boot` 模块，该模块展示了如何使用 `quartz-plus-core` 模块。
