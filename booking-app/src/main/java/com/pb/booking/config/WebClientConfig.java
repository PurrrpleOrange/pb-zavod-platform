package com.pb.booking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final BookingProperties bookingProperties;

    @Bean
    public WebClient schedulingWebClient() {
        return WebClient.builder()
                .baseUrl(bookingProperties.getSchedulingBaseUrl())
                .build();
    }
}
