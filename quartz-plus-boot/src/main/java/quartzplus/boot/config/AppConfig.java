package quartzplus.boot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import quartzplus.core.dto.JobRequest;
import quartzplus.core.service.QuartzPlusBaseService;

import java.util.UUID;

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
}
