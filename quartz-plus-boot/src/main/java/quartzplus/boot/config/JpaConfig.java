package quartzplus.boot.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
public class JpaConfig {

    @Bean
    static BeanPostProcessor entityManagerFactoryBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof LocalContainerEntityManagerFactoryBean emf) {
                    emf.setEntityManagerFactoryInterface(EntityManagerFactory.class);
                }
                return bean;
            }
        };
    }
}
