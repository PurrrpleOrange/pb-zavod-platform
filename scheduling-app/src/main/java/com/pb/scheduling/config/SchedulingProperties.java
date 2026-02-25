package com.pb.scheduling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pb.scheduling")
public record SchedulingProperties(int holdMinutes) {}
