package com.pb.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "pb.gateway")
public class GatewayProperties {

    private String jwtSecret;
}
