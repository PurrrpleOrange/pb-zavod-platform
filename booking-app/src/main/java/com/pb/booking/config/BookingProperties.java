package com.pb.booking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "pb.booking")
public class BookingProperties {

    private String schedulingBaseUrl = "http://localhost:8081";
    private int holdExpiryMinutes = 15;
    private long expiryCheckIntervalMs = 60000;
}
