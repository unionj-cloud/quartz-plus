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
package quartzplus.core.service;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import quartzplus.core.dto.GroupRequest;
import quartzplus.core.dto.JobRequest;

public interface QuartzPlusBaseService {

    /**
     * Add scheduled job
     */
    boolean addScheduleJob(JobRequest jobRequest);

    /**
     * Update scheduled job
     */
    boolean updateScheduleJob(JobRequest jobRequest);

    /**
     * Delete scheduled job
     */
    boolean deleteScheduleJob(JobRequest jobRequest);

    /**
     * Pause scheduled job
     */
    boolean pauseScheduleJob(JobRequest jobRequest);

    /**
     * Resume scheduled job
     */
    boolean resumeScheduleJob(JobRequest jobRequest);

    /**
     * Execute job immediately
     */
    boolean immediatelyJob(JobRequest jobRequest);

    /**
     * Check if job is running
     */
    boolean isJobRunning(JobRequest jobRequest);

    /**
     * Check if job exists
     */
    boolean isJobExists(JobRequest jobRequest);

    /**
     * Get schedule state
     */
    String getScheduleState(JobRequest jobRequest);

    /**
     * Create JobDetail
     */
    JobDetail createJob(JobRequest jobRequest, Class<? extends Job> jobClass, org.springframework.context.ApplicationContext context);

    /**
     * Create Trigger
     */
    Trigger createTrigger(JobRequest jobRequest);

    /**
     * Pause group
     */
    boolean pauseGroup(GroupRequest groupRequest);

    /**
     * Resume group
     */
    boolean resumeGroup(GroupRequest groupRequest);

    /**
     * Stop running job
     */
    boolean stopRunningJob(JobRequest jobRequest);
}
