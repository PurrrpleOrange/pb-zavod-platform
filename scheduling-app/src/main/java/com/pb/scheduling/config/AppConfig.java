package com.pb.scheduling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock(SchedulingProperties props) {
        return Clock.system(ZoneId.of(props.timezone()));
    }
}
