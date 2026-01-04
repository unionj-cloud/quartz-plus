package quartzplus.boot;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import quartzplus.core.spring.QuartzPlusJobScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@QuartzPlusJobScan("quartzplus.boot.job")
@RequiredArgsConstructor
public class BootApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootApplication.class, args);
    }
}
