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

import lombok.Data;

@Data
public class SchedulerConfig {

    /**
     * 是否启用该调度器
     */
    private Boolean enabled = false;

    /**
     * 调度器实例名称
     */
    private String instanceName;

    /**
     * 线程池线程数
     */
    private Integer threadCount = 3;

    /**
     * 线程名称前缀
     */
    private String threadNamePrefix;

    /**
     * 数据库表前缀
     */
    private String tablePrefix;

    /**
     * 是否开启集群模式
     */
    private Boolean clustered = false;

    /**
     * 调度器描述
     */
    private String description;

    private Boolean autoStartup = false;

}
