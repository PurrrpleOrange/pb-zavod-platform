package com.pb.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();
    private Refresh refresh = new Refresh();

    @Data
    public static class Jwt {
        private String issuer = "auth-service";
        private String accessSecret;
        private Duration accessTtl = Duration.ofMinutes(15);
    }

    @Data
    public static class Refresh {
        private Duration ttl = Duration.ofDays(14);
    }
}
