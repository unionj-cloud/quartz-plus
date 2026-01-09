package quartzplus.boot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import quartzplus.core.dto.JobRequest;
import quartzplus.core.service.QuartzPlusBaseService;

import java.util.UUID;

@Slf4j
@Configuration
public class AppConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private QuartzPlusBaseService quartzPlusBaseService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
//        ConfigurableListableBeanFactory beanFactory = event.getApplicationContext().getBeanFactory();
//        beanFactory.getBeanNamesIterator().forEachRemaining(beanName -> {
//            if (StringUtils.containsIgnoreCase(beanName, "simpleJob")) {
//                Object bean = beanFactory.getBean(beanName);
//                System.out.println(bean);
//            }
//        });

        JobRequest jobRequest = new JobRequest();
        jobRequest.setJobName("testSimpleJobA" + UUID.randomUUID());
        jobRequest.setJobClass("quartzplus.boot.job.TestSimpleJobA");
        quartzPlusBaseService.addScheduleJob(jobRequest);
    }

    @Bean
    @Order(0)
    public SchedulerFactoryBeanCustomizer dataSourceCustomizer() {
        return (schedulerFactoryBean) -> {
//            schedulerFactoryBean.setAutoStartup(false);
            log.info("do something customizing to schedulerFactoryBean" + schedulerFactoryBean);
        };
    }
}
