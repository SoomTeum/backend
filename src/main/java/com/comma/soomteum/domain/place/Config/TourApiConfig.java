package com.comma.soomteum.domain.place.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
@RequiredArgsConstructor
public class TourApiConfig {

    private final WebClient.Builder builder;
    private final TourApiProperties props;

    @Bean
    public Map<String, WebClient> tourApiClients() {
        var map = new HashMap<String, WebClient>();
        var connector = reactorConnector(props.getTimeout().getConnectMs(), props.getTimeout().getReadMs());

        props.getClients().forEach((key, c) -> {
            var wc = builder
                    .baseUrl(c.getBaseUrl())
                    .clientConnector(connector)
                    .build();
            map.put(key, wc);
            log.info("Registered TourAPI client: {} -> {}", key, c.getBaseUrl());
        });

        return map;
    }

    private org.springframework.http.client.reactive.ReactorClientHttpConnector reactorConnector(int connectMs, int readMs) {
        var http = reactor.netty.http.client.HttpClient.create()
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMs)
                .responseTimeout(java.time.Duration.ofMillis(readMs));
        return new org.springframework.http.client.reactive.ReactorClientHttpConnector(http);
    }

    @Bean
    public Function<String, String> tourApiKeyResolver() {
        return key -> {
            var client = props.getClients().get(key);
            if (client != null && client.getServiceKey() != null && !client.getServiceKey().isBlank()) {
                return client.getServiceKey();
            }
            return props.getDefaults().getServiceKey();
        };
    }

    @Bean
    public BiConsumer<org.springframework.web.util.UriBuilder, TourApiProperties> commonQueryApplier() {
        return (b, p) -> {
            if (p.getCommon().getMobileOs() != null) {
                b.queryParam("MobileOS", p.getCommon().getMobileOs());
            }
            if (p.getCommon().getMobileApp() != null) {
                b.queryParam("MobileApp", p.getCommon().getMobileApp());
            }
            b.queryParam("_type", "json");
        };
    }
}
