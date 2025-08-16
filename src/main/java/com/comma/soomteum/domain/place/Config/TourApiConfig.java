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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
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

            // ✅ 값 인코딩 활성화 (serviceKey 등 자동 인코딩)
            DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory(clientProps.getBaseUrl());
            uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.TEMPLATE_AND_VALUES);

            WebClient client = WebClient.builder()
                    .uriBuilderFactory(uriFactory)
                    .baseUrl(clientProps.getBaseUrl())
                    .defaultHeaders(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)))
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

    @Bean
    public Function<String, String> tourApiKeyResolver() {
        return key -> {
            // ✅ props에 이미 %2B 같은 "인코딩된" 키가 있다면 한 번 디코딩해서 '생키'로 사용
            String k = null;
            var client = props.getClients().get(key);
            if (client != null && client.getServiceKey() != null && !client.getServiceKey().isBlank()) {
                k = client.getServiceKey();
            } else {
                k = props.getDefaults().getServiceKey();
            }
            if (k != null && k.contains("%")) {
                try { k = URLDecoder.decode(k, StandardCharsets.UTF_8); } catch (Exception ignore) {}
            }
            return k; // UriBuilder가 알아서 인코딩해 줍니다.
        };
    }

    @Bean
    public BiConsumer<org.springframework.web.util.UriBuilder, TourApiProperties> commonQueryApplier() {
        return (b, p) -> {
            String mobileOs  = (p.getCommon() != null && p.getCommon().getMobileOs()  != null)
                    ? p.getCommon().getMobileOs()  : "ETC";
            String mobileApp = (p.getCommon() != null && p.getCommon().getMobileApp() != null)
                    ? p.getCommon().getMobileApp() : "soomteum";

            b.queryParam("MobileOS",  mobileOs);
            b.queryParam("MobileApp", mobileApp);
            b.queryParam("_type",     "json");
        };
    }
}
