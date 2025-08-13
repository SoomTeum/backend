package com.comma.soomteum.domain.place.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
@RequiredArgsConstructor
public class TourApiConfig {

    private final TourApiProperties props;

    @Bean
    @Qualifier("tourApiClients")
    public Map<String, WebClient> tourApiClients(TourApiProperties props) {
        Map<String, WebClient> clients = new HashMap<>();

        props.getClients().forEach((key, clientProps) -> {

            // ★ 인코딩 모드 해제된 UriBuilderFactory 생성
            DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory(clientProps.getBaseUrl());
            uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

            WebClient client = WebClient.builder()
                    .uriBuilderFactory(uriFactory)
                    .baseUrl(clientProps.getBaseUrl())
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    // 요청 로깅
                    .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                        System.out.println(">> [" + key + "] " + req.method() + " " + req.url());
                        System.out.println(">> Accept=" + req.headers().getFirst(HttpHeaders.ACCEPT));
                        return Mono.just(req);
                    }))
                    // 응답 로깅
                    .filter(ExchangeFilterFunction.ofResponseProcessor(resp -> {
                        System.out.println("<< [" + key + "] Status=" + resp.statusCode());
                        System.out.println("<< Content-Type=" +
                                resp.headers().asHttpHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
                        return Mono.just(resp);
                    }))
                    .build();

            clients.put(key, client);
        });

        return clients;
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
