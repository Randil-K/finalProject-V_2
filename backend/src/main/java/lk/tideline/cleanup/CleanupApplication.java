package lk.tideline.cleanup;

import lk.tideline.cleanup.config.TidelineProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(TidelineProperties.class)
@EnableScheduling
public class CleanupApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleanupApplication.class, args);
    }
}
