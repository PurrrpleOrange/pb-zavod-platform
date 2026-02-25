package com.pb.scheduling;

import com.pb.scheduling.config.SchedulingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SchedulingProperties.class)
public class SchedulingAppliaction {
    public static void main(String[] args) {
        SpringApplication.run(SchedulingAppliaction.class, args);
    }
}
