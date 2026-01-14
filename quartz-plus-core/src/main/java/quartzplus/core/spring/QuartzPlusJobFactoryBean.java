/*
 * All content copyright unionj-cloud, unless otherwise indicated. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package quartzplus.core.spring;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.quartz.v2_0.QuartzTelemetry;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobListener;
import org.quartz.TriggerListener;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import quartzplus.core.quartz.QuartzUtils;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Properties;

@Slf4j
public class QuartzPlusJobFactoryBean implements FactoryBean, InitializingBean, DisposableBean, SmartLifecycle {

    private Class jobBeanClass;

    @Autowired
    private QuartzProperties quartzProperties;
    @Autowired(required = false)
    private DataSource dataSource;
    @Autowired(required = false)
    private JobListener jobsListener;
    @Autowired(required = false)
    private TriggerListener triggersListener;
    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Environment environment;
    @Autowired
    private ObjectProvider<SchedulerFactoryBeanCustomizer> customizers;
    @Autowired(required = false)
    private OpenTelemetry openTelemetry;

    private SchedulerFactoryBean schedulerFactoryBean;

    public QuartzPlusJobFactoryBean(Class jobBeanClass) {
        this.jobBeanClass = jobBeanClass;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        schedulerFactoryBean = createSchedulerFactoryBean(jobBeanClass, applicationContext);
        if (Objects.isNull(schedulerFactoryBean)) {
            return;
        }
        QuartzUtils.addSchedulerFactoryBean(jobBeanClass.getName(), schedulerFactoryBean);

        schedulerFactoryBean.afterPropertiesSet();

        if (Objects.nonNull(openTelemetry)) {
            QuartzTelemetry quartzTelemetry = QuartzTelemetry.builder(openTelemetry).setCaptureExperimentalSpanAttributes(true).build();
            quartzTelemetry.configure(schedulerFactoryBean.getScheduler());
        }
    }

    @Override
    public Object getObject() throws Exception {
        return schedulerFactoryBean;
    }

    @Override
    public Class<?> getObjectType() {
        return SchedulerFactoryBean.class;
    }

    private SchedulerFactoryBean createSchedulerFactoryBean(Class jobBeanClass, ApplicationContext applicationContext) throws Exception {
        SchedulerConfig config = loadSchedulerConfig(jobBeanClass);
        if (!config.getEnabled()) {
            return null;
        }

        // Use default value if instance name is not configured
        String schedulerName = config.getInstanceName() != null ? config.getInstanceName() : jobBeanClass.getSimpleName() + "Scheduler";

        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();

        // Basic configuration
        schedulerFactoryBean.setAutoStartup(config.getAutoStartup());
        schedulerFactoryBean.setSchedulerName(schedulerName);
        schedulerFactoryBean.setOverwriteExistingJobs(true);
        schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true);

        // Job factory configuration
        AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        schedulerFactoryBean.setJobFactory(jobFactory);
        schedulerFactoryBean.setApplicationContext(applicationContext);

        // DataSource and transaction manager
        if (Objects.nonNull(dataSource)) {
            schedulerFactoryBean.setDataSource(dataSource);
        }
        if (Objects.nonNull(transactionManager)) {
            schedulerFactoryBean.setTransactionManager(transactionManager);
        }

        schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext");

        // Listener configuration
        if (Objects.nonNull(jobsListener)) {
            schedulerFactoryBean.setGlobalJobListeners(jobsListener);
        }
        if (Objects.nonNull(triggersListener)) {
            schedulerFactoryBean.setGlobalTriggerListeners(triggersListener);
        }

        // Custom properties configuration - read from SchedulerProperties configuration class
        Properties properties = new Properties();
        properties.putAll(quartzProperties.getProperties());

        // Thread pool configuration
        String threadNamePrefix = config.getThreadNamePrefix() != null ?
            config.getThreadNamePrefix() : schedulerName + "_";
        properties.setProperty("org.quartz.threadPool.threadNamePrefix", threadNamePrefix);
        properties.setProperty("org.quartz.threadPool.threadCount", String.valueOf(config.getThreadCount()));

        // Scheduler instance configuration
        properties.setProperty("org.quartz.scheduler.instanceName", schedulerName);
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");

        // JobStore configuration (use independent table prefix)
        String tablePrefix = config.getTablePrefix() != null ?
            config.getTablePrefix() : "QRTZ_" + schedulerName.toUpperCase().replace("SCHEDULER", "") + "_";
        properties.setProperty("org.quartz.jobStore.tablePrefix", tablePrefix);
        properties.setProperty("org.quartz.jobStore.isClustered", String.valueOf(config.getClustered()));

        schedulerFactoryBean.setQuartzProperties(properties);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(schedulerFactoryBean));

        log.info("Created scheduler: {} - type: {}, thread count: {}, table prefix: {}, clustered: {}, auto startup: {}",
            schedulerName, config.getDescription(), config.getThreadCount(),
            tablePrefix, config.getClustered(), config.getAutoStartup());

        return schedulerFactoryBean;
    }

    private SchedulerConfig loadSchedulerConfig(Class jobBeanClass) {
        SchedulerConfig config = new SchedulerConfig();

        // Get @QuartzPlusJob annotation value
        String jobName = null;
        if (jobBeanClass.isAnnotationPresent(QuartzPlusJob.class)) {
            QuartzPlusJob annotation = (QuartzPlusJob) jobBeanClass.getAnnotation(QuartzPlusJob.class);
            jobName = annotation.value();
        }

        // If annotation value is empty, convert class name to kebab-case
        if (jobName == null || jobName.isEmpty()) {
            jobName = camelToKebabCase(jobBeanClass.getSimpleName());
        } else {
            // Convert camelCase to kebab-case
            jobName = camelToKebabCase(jobName);
        }

        // Build configuration prefix
        String configPrefix = "quartz.scheduler." + jobName;

        // Read configuration from configuration file
        config.setEnabled(environment.getProperty(configPrefix + ".enabled", Boolean.class, config.getEnabled()));
        config.setInstanceName(environment.getProperty(configPrefix + ".instance-name", config.getInstanceName()));
        config.setThreadCount(environment.getProperty(configPrefix + ".thread-count", Integer.class, config.getThreadCount()));
        config.setThreadNamePrefix(environment.getProperty(configPrefix + ".thread-name-prefix", config.getThreadNamePrefix()));
        config.setTablePrefix(environment.getProperty(configPrefix + ".table-prefix", config.getTablePrefix()));
        config.setClustered(environment.getProperty(configPrefix + ".clustered", Boolean.class, config.getClustered()));
        config.setDescription(environment.getProperty(configPrefix + ".description", config.getDescription()));
        config.setAutoStartup(environment.getProperty(configPrefix + ".auto-startup", Boolean.class, config.getAutoStartup()));

        return config;
    }

    private String camelToKebabCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('-');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    @Override
    public void destroy() throws Exception {
        if (schedulerFactoryBean != null) {
            schedulerFactoryBean.destroy();
        }
    }

    @Override
    public void start() {
        if (schedulerFactoryBean != null) {
            if (schedulerFactoryBean.isAutoStartup()) {
                schedulerFactoryBean.start();
            }
        }
    }

    @Override
    public void stop() {
        if (schedulerFactoryBean != null) {
            schedulerFactoryBean.stop();
        }
    }

    @Override
    public boolean isRunning() {
        if (schedulerFactoryBean != null) {
            return schedulerFactoryBean.isRunning();
        }
        return false;
    }

    @Override
    public boolean isAutoStartup() {
        if (schedulerFactoryBean != null) {
            return schedulerFactoryBean.isAutoStartup();
        }
        return false;
    }

    @Override
    public int getPhase() {
        if (schedulerFactoryBean != null) {
            return schedulerFactoryBean.getPhase();
        }
        return SmartLifecycle.super.getPhase();
    }
}
