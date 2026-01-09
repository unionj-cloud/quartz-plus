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
package quartzplus.core;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import quartzplus.core.service.QuartzPlusBaseService;
import quartzplus.core.service.impl.DefaultQuartzPlusBaseServiceImpl;

@Configuration
@Import(QuartzProperties.class)
@RequiredArgsConstructor
public class QuartzPlusCoreAutoConfiguration {

    private final ApplicationContext context;

    @Bean
    @ConditionalOnMissingBean
    public QuartzPlusBaseService getQuartzPlusBaseService() {
        return new DefaultQuartzPlusBaseServiceImpl(context);
    }
}
