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
package quartzplus.core.quartz;

import cn.hutool.core.util.StrUtil;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.util.ObjectUtils;
import quartzplus.core.dto.GroupRequest;
import quartzplus.core.dto.JobRequest;

import java.text.ParseException;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.quartz.CronExpression.isValidExpression;

public class QuartzUtils {

    private static final Map<String, SchedulerFactoryBean> schedulerFactoryBeans = new ConcurrentHashMap<>();

    public static void addSchedulerFactoryBean(String jobBeanClassName, SchedulerFactoryBean schedulerFactoryBean) {
        schedulerFactoryBeans.put(jobBeanClassName, schedulerFactoryBean);
    }

    public static Scheduler getScheduler(JobRequest jobRequest) throws SchedulerException {
        SchedulerFactoryBean schedulerFactoryBean = schedulerFactoryBeans.get(jobRequest.getJobClass());
        if (ObjectUtils.isEmpty(schedulerFactoryBean)) {
            String errMsg = StrUtil.format("Scheduler for job {} not found", jobRequest.getJobClass());
            throw new SchedulerException(errMsg);
        }
        return schedulerFactoryBean.getScheduler();
    }

    public static Scheduler getScheduler(GroupRequest groupRequest) throws SchedulerException {
        SchedulerFactoryBean schedulerFactoryBean = schedulerFactoryBeans.get(groupRequest.getJobClass());
        if (ObjectUtils.isEmpty(schedulerFactoryBean)) {
            String errMsg = StrUtil.format("Scheduler for job {} not found", groupRequest.getJobClass());
            throw new SchedulerException(errMsg);
        }
        return schedulerFactoryBean.getScheduler();
    }

    public static JobDetail createJob(JobRequest jobRequest, Class<? extends Job> jobClass, ApplicationContext context) {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(jobClass);
        factoryBean.setDurability(false);
        factoryBean.setApplicationContext(context);
        factoryBean.setName(jobRequest.getJobName());
        factoryBean.setGroup(jobRequest.getJobGroup());
        factoryBean.setDescription(jobRequest.getDesc());
        factoryBean.setRequestsRecovery(true);
        if (jobRequest.getJobDataMap() != null) {
            factoryBean.setJobDataMap(jobRequest.getJobDataMap());
        }

        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    public static Trigger createTrigger(JobRequest jobRequest) {
        String cronExpression = jobRequest.getCronExpression();
        if (cronExpression != null) {
            if (isValidExpression(cronExpression)) {
                return createCronTrigger(jobRequest);
            }
            throw new IllegalArgumentException("Provided expression " + cronExpression + " is not a valid cron expression");
        } else {
            return createSimpleTrigger(jobRequest);
        }
    }

    private static Trigger createCronTrigger(JobRequest jobRequest) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("failCnt", "0");
        jobDataMap.put("stop", "N");
        jobDataMap.put("retry", jobRequest.getRetry());
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setName(jobRequest.getJobName().concat("Trigger"));
        factoryBean.setGroup(jobRequest.getJobGroup());
        factoryBean.setCronExpression(jobRequest.getCronExpression());
        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
        factoryBean.setJobDataMap(jobDataMap);
        try {
            factoryBean.afterPropertiesSet();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return factoryBean.getObject();
    }

    private static Trigger createSimpleTrigger(JobRequest jobRequest) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setName(jobRequest.getJobName());
        factoryBean.setGroup(jobRequest.getJobGroup());

        if (!ObjectUtils.isEmpty(jobRequest.getStartDateAt())) {
            Date startTime = Date.from(jobRequest.getStartDateAt().atZone(ZoneId.systemDefault()).toInstant());
            factoryBean.setStartTime(startTime);
        }

        // Use IGNORE strategy: do not modify nextFireTime on misfire, keep the original trigger time
        // This ensures tasks execute in the original submission order, even if misfire occurs
        factoryBean.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY);
        factoryBean.setRepeatInterval(jobRequest.getRepeatIntervalInSeconds() * 1000); //ms
        factoryBean.setRepeatCount(jobRequest.getRepeatCount());

        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
