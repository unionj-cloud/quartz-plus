package quartzplus.boot.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import quartzplus.core.spring.QuartzPlusJob;

@Slf4j
@QuartzPlusJob("simpleJobB")
public class TestSimpleJobB extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("============================================================================");
        log.info("TestSimpleJobB");
        log.info("============================================================================");
    }
}
