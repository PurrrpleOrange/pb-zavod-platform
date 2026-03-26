package com.pb.scheduling.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pb.scheduling")
public record SchedulingProperties(
        @Min(1) int holdMinutes,
        @Min(1) int defaultDurationMinutesForZones,
        String timezone
) {}
