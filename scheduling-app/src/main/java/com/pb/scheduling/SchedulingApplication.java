package com.pb.scheduling;

import com.pb.scheduling.config.SchedulingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(SchedulingProperties.class)
@EnableScheduling
public class SchedulingApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulingApplication.class, args);
    }
}
