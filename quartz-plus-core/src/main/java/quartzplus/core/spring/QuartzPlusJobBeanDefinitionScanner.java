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

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.Set;

public class QuartzPlusJobBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    public QuartzPlusJobBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        super(registry);
    }

    @Override
    protected void registerBeanDefinition(BeanDefinitionHolder oldHolder, BeanDefinitionRegistry registry) {
        String beanName = oldHolder.getBeanName();

        BeanDefinition beanDefinition = oldHolder.getBeanDefinition();
        if (beanDefinition instanceof AnnotatedBeanDefinition) {
            AnnotatedBeanDefinition abd = (AnnotatedBeanDefinition) beanDefinition;
            AnnotationMetadata metadata = abd.getMetadata();

            Map<String, Object> attributes = metadata.getAnnotationAttributes(QuartzPlusJob.class.getName());

            if (attributes != null) {
                String value = (String) attributes.get("value");
                if (value != null && !value.isEmpty()) {
                    beanName = value;
                }
            }
        }

        BeanDefinitionHolder newHolder = new BeanDefinitionHolder(
            beanDefinition,
            beanName,
            oldHolder.getAliases()
        );

        BeanDefinitionReaderUtils.registerBeanDefinition(newHolder, registry);
    }

    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitionHolders = super.doScan(basePackages);

        for (BeanDefinitionHolder beanDefinitionHolder : beanDefinitionHolders) {
            BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();

            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition.getBeanClassName());
            beanDefinition.setBeanClassName(QuartzPlusJobFactoryBean.class.getName());
            beanDefinition.setLazyInit(false);
        }

        return beanDefinitionHolders;
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return true;
    }
}
