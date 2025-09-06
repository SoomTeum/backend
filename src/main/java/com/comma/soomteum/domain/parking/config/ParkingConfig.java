package com.comma.soomteum.domain.parking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ParkingConfig {

    @Bean(name = "parkingWebClient")
    public WebClient parkingWebClient() {
        return WebClient.builder()
                .baseUrl("https://apis.data.go.kr/B553881/parkingInfo")
                .build();
    }
}