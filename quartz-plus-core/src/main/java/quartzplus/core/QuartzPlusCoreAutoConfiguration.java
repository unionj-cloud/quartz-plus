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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSourceInitializer;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import quartzplus.core.service.QuartzPlusBaseService;
import quartzplus.core.service.impl.DefaultQuartzPlusBaseServiceImpl;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(QuartzProperties.class)
@RequiredArgsConstructor
public class QuartzPlusCoreAutoConfiguration {

    private final ApplicationContext context;

    @Bean
    @ConditionalOnMissingBean
    public QuartzPlusBaseService getQuartzPlusBaseService() {
        return new DefaultQuartzPlusBaseServiceImpl(context);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnSingleCandidate(DataSource.class)
    @ConditionalOnProperty(prefix = "spring.quartz", name = "job-store-type", havingValue = "jdbc")
    @Import(DatabaseInitializationDependencyConfigurer.class)
    protected static class JdbcStoreTypeConfiguration {

        private DataSource getDataSource(DataSource dataSource, ObjectProvider<DataSource> quartzDataSource) {
            DataSource dataSourceIfAvailable = quartzDataSource.getIfAvailable();
            return (dataSourceIfAvailable != null) ? dataSourceIfAvailable : dataSource;
        }

        @Bean
        @SuppressWarnings("deprecation")
        @ConditionalOnMissingBean({QuartzDataSourceScriptDatabaseInitializer.class,
            QuartzDataSourceInitializer.class})
        @Conditional(QuartzPlusCoreAutoConfiguration.JdbcStoreTypeConfiguration.OnQuartzDatasourceInitializationCondition.class)
        public QuartzDataSourceScriptDatabaseInitializer quartzDataSourceScriptDatabaseInitializer(
            DataSource dataSource, @QuartzDataSource ObjectProvider<DataSource> quartzDataSource,
            QuartzProperties properties) {
            DataSource dataSourceToUse = getDataSource(dataSource, quartzDataSource);
            return new QuartzDataSourceScriptDatabaseInitializer(dataSourceToUse, properties);
        }

        static class OnQuartzDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

            OnQuartzDatasourceInitializationCondition() {
                super("Quartz", "spring.quartz.jdbc.initialize-schema");
            }

        }
    }
}
