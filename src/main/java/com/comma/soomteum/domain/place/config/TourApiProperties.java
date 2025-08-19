package com.comma.soomteum.domain.place.config;


import lombok.*;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "tourapi")
public class TourApiProperties {

    @Builder.Default
    private Defaults defaults = new Defaults();
    @Builder.Default
    private Timeout timeout = new Timeout();
    @Builder.Default
    private Common common = new Common();
    @Builder.Default
    private Map<String, Client> clients = new HashMap<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Defaults {
        private String serviceKey;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Timeout {
        @Builder.Default
        private int connectMs = 3000;
        @Builder.Default
        private int readMs = 5000;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Common {
        private String mobileOs;
        private String mobileApp;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Client {
        private String baseUrl;
        private String serviceKey;
    }
}