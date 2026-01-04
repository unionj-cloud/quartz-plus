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
package quartzplus.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.ApplicationContext;
import quartzplus.core.dto.GroupRequest;
import quartzplus.core.dto.JobRequest;
import quartzplus.core.quartz.QuartzUtils;
import quartzplus.core.service.QuartzPlusBaseService;

import java.util.Date;
import java.util.List;

import static quartzplus.core.quartz.QuartzUtils.getScheduler;

@Slf4j
@RequiredArgsConstructor
public class DefaultQuartzPlusBaseServiceImpl implements QuartzPlusBaseService {

    private final ApplicationContext context;

    @Override
    @SneakyThrows
    public boolean addScheduleJob(JobRequest jobRequest) {
        Scheduler scheduler = getScheduler(jobRequest);

        try {
            Class<Job> jobClass = (Class<Job>) Class.forName(jobRequest.getJobClass());
            JobDetail jobDetail = createJob(jobRequest, jobClass, context);
            Trigger trigger = createTrigger(jobRequest);

            scheduler.scheduleJob(jobDetail, trigger);

            jobRequest.setSchedName(scheduler.getSchedulerName());

            log.info("Successfully added job {} to scheduler {}", jobRequest.getJobName(), scheduler.getSchedulerName());
            return true;
        } catch (ObjectAlreadyExistsException e) {
            log.warn(e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    @SneakyThrows
    public boolean updateScheduleJob(JobRequest jobRequest) {
        Scheduler scheduler = getScheduler(jobRequest);
        JobKey jobKey = JobKey.jobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        Trigger newTrigger = createTrigger(jobRequest);
        Date dt = scheduler.rescheduleJob(
            TriggerKey.triggerKey(jobRequest.getJobName().concat("Trigger"), jobRequest.getJobGroup()),
            newTrigger
        );
        log.debug("Job {} rescheduled successfully at date: {}", jobKey, dt);
        return true;
    }

    @Override
    @SneakyThrows
    public boolean deleteScheduleJob(JobRequest jobRequest) {
        Scheduler scheduler = getScheduler(jobRequest);
        JobKey jobKey = JobKey.jobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        boolean deleted = scheduler.deleteJob(jobKey);
        log.info("Job {} deleted from scheduler {}: {}", jobRequest.getJobName(), scheduler.getSchedulerName(), deleted);
        return deleted;
    }

    @Override
    @SneakyThrows
    public boolean pauseScheduleJob(JobRequest jobRequest) {
        Scheduler scheduler = getScheduler(jobRequest);
        JobKey jobKey = JobKey.jobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        scheduler.pauseJob(jobKey);
        log.info("Job {} paused in scheduler {}", jobRequest.getJobName(), scheduler.getSchedulerName());
        return true;
    }

    @Override
    @SneakyThrows
    public boolean resumeScheduleJob(JobRequest jobRequest) {
        Scheduler scheduler = getScheduler(jobRequest);
        JobKey jobKey = JobKey.jobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        scheduler.resumeJob(jobKey);
        log.info("Job {} resumed in scheduler {}", jobRequest.getJobName(), scheduler.getSchedulerName());
        return true;
    }

    @Override
    @SneakyThrows
    public boolean immediatelyJob(JobRequest jobRequest) {
        Scheduler scheduler = getScheduler(jobRequest);
        JobKey jobKey = JobKey.jobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        JobDataMap jobDataMap = new JobDataMap(jobRequest.getJobDataMap());
        scheduler.triggerJob(jobKey, jobDataMap);
        log.info("Job {} triggered immediately in scheduler {}", jobRequest.getJobName(), scheduler.getSchedulerName());
        return true;
    }

    @Override
    @SneakyThrows
    public boolean isJobRunning(JobRequest jobRequest) {
        Scheduler scheduler = getScheduler(jobRequest);
        List<JobExecutionContext> currentJobs = scheduler.getCurrentlyExecutingJobs();
        if (currentJobs != null) {
            for (JobExecutionContext jobCtx : currentJobs) {
                if (jobRequest.getJobName().equals(jobCtx.getJobDetail().getKey().getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @SneakyThrows
    public boolean isJobExists(JobRequest jobRequest) {
        Scheduler scheduler = getScheduler(jobRequest);
        JobKey jobKey = JobKey.jobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        return scheduler.checkExists(jobKey);
    }

    @Override
    @SneakyThrows
    public String getScheduleState(JobRequest jobRequest) {
        Scheduler scheduler = getScheduler(jobRequest);
        JobKey jobKey = JobKey.jobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());

        if (triggers != null && triggers.size() > 0) {
            for (Trigger trigger : triggers) {
                Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                if (Trigger.TriggerState.NORMAL.equals(triggerState)) {
                    return "SCHEDULED";
                }
                return triggerState.name().toUpperCase();
            }
        }
        return null;
    }

    @Override
    public JobDetail createJob(JobRequest jobRequest, Class<? extends Job> jobClass, ApplicationContext context) {
        return QuartzUtils.createJob(jobRequest, jobClass, context);
    }

    @Override
    public Trigger createTrigger(JobRequest jobRequest) {
        return QuartzUtils.createTrigger(jobRequest);
    }

    @Override
    @SneakyThrows
    public boolean pauseGroup(GroupRequest groupRequest) {
        log.info("Pausing group, jobGroup={}, jobClass={}", groupRequest.getJobGroup(), groupRequest.getJobClass());
        Scheduler scheduler = getScheduler(groupRequest);
        scheduler.pauseTriggers(GroupMatcher.groupEquals(groupRequest.getJobGroup()));
        return true;
    }

    @Override
    @SneakyThrows
    public boolean resumeGroup(GroupRequest groupRequest) {
        log.info("Resuming group, jobGroup={}, jobClass={}", groupRequest.getJobGroup(), groupRequest.getJobClass());
        Scheduler scheduler = getScheduler(groupRequest);
        scheduler.resumeTriggers(GroupMatcher.groupEquals(groupRequest.getJobGroup()));
        return true;
    }

    @Override
    @SneakyThrows
    public boolean stopRunningJob(JobRequest jobRequest) {
        log.info("Stopping running job, jobGroup={}, jobClass={}, jobName={}", jobRequest.getJobGroup(), jobRequest.getJobClass(), jobRequest.getJobName());
        Scheduler scheduler = getScheduler(jobRequest);
        JobKey jobKey = JobKey.jobKey(jobRequest.getJobName(), jobRequest.getJobGroup());
        return scheduler.interrupt(jobKey);
    }
}
