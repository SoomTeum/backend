package com.comma.soomteum.domain.external.tourapi.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
            DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory(clientProps.getBaseUrl());
            uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);

            ExchangeFilterFunction reqLog = ExchangeFilterFunction.ofRequestProcessor(req -> {
                String masked = req.url().toString().replaceAll("(serviceKey=)([^&]+)", "$1****");
                System.out.println(">> [" + key + "] " + req.method() + " " + masked);
                System.out.println(">> Accept=" + req.headers().getFirst(HttpHeaders.ACCEPT));
                return Mono.just(req);
            });

            ExchangeFilterFunction respLog = ExchangeFilterFunction.ofResponseProcessor(resp -> {
                System.out.println("<< [" + key + "] Status=" + resp.statusCode());
                System.out.println("<< Content-Type=" +
                        resp.headers().asHttpHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
                return Mono.just(resp);
            });

            WebClient client = WebClient.builder()
                    .uriBuilderFactory(uriFactory)
                    .baseUrl(clientProps.getBaseUrl())
                    .defaultHeaders(h -> h.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)))
                    .filter(reqLog)
                    .filter(respLog)
                    .build();

            clients.put(key, client);
        });

        return clients;
    }

    /**
     * 서비스키 리졸버
     * - 설정에 인코딩(%.. )된 값이 들어와도 한 번만 디코딩해서 raw로 통일
     * - 최종 전송 시 인코딩은 UriBuilder가 수행
     */
    @Bean
    public Function<String, String> tourApiKeyResolver() {
        return key -> {
            var c = props.getClients().get(key);
            String k = (c != null && c.getServiceKey() != null)
                    ? c.getServiceKey()
                    : (props.getDefaults() != null ? props.getDefaults().getServiceKey() : null);
            if (k == null) return null;
            // 설정에 인코딩된 키가 들어왔으면 raw로 통일
            if (k.contains("%")) {
                try { k = URLDecoder.decode(k, StandardCharsets.UTF_8); } catch (Exception ignore) {}
            }
            return k; // ★ raw 키 반환 (인코딩은 UriBuilder가 함)
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
