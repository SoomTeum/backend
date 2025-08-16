package com.comma.soomteum.domain.place.Service;

import com.comma.soomteum.domain.place.Config.TourApiProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
@Component
@Slf4j
public class KorApiCaller {

    private static final String CLIENT_KEY = "korService2";

    @Qualifier("tourApiClients")
    private final Map<String, WebClient> tourApiClients;

    private final TourApiProperties props;
    private final BiConsumer<UriBuilder, TourApiProperties> commonQueryApplier;
    private final Function<String, String> tourApiKeyResolver;

    // JSON: { "response": {...} } 언랩 ON
    private final ObjectMapper objectMapper = new ObjectMapper();
    {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
        objectMapper.findAndRegisterModules();
    }

    // XML 파서
    private final XmlMapper xmlMapper = new XmlMapper();
    {
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.findAndRegisterModules();
    }

    public <T> Mono<T> get(String path, Consumer<UriBuilder> queryBuilder, Class<T> bodyType) {
        WebClient client = Objects.requireNonNull(
                tourApiClients.get(CLIENT_KEY), "WebClient not found for key: " + CLIENT_KEY);

        String serviceKey = tourApiKeyResolver.apply(CLIENT_KEY);

        return client.get()
                .uri(b -> {
                    b.path(path).queryParam("serviceKey", serviceKey);
                    queryBuilder.accept(b);
                    commonQueryApplier.accept(b, props); // (_type=json 등 공통 쿼리)
                    return b.build();
                })
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .exchangeToMono(resp -> handleResponse(resp, bodyType))
                .doOnSubscribe(s -> log.debug(">> [{}] GET {}", CLIENT_KEY, path))
                .doOnError(WebClientResponseException.class,
                        ex -> log.error("[{}] HTTP error: {} {}", CLIENT_KEY,
                                ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex))
                .doOnError(ex -> log.error("[{}] Unexpected error", CLIENT_KEY, ex));
    }

    private <T> Mono<T> handleResponse(ClientResponse resp, Class<T> bodyType) {
        if (resp.statusCode().is4xxClientError()) {
            return resp.bodyToMono(String.class).defaultIfEmpty("")
                    .doOnNext(body -> log.warn("[{}] 4xx: {}", CLIENT_KEY, mask(body)))
                    .flatMap(body -> Mono.error(new Upstream4xxException("TourAPI 4xx", body)));
        }
        if (resp.statusCode().is5xxServerError()) {
            return resp.bodyToMono(String.class).defaultIfEmpty("")
                    .doOnNext(body -> log.error("[{}] 5xx: {}", CLIENT_KEY, mask(body)))
                    .flatMap(body -> Mono.error(new Upstream5xxException("TourAPI 5xx", body)));
        }

        MediaType ct = resp.headers().contentType().orElse(null);

        return resp.bodyToMono(String.class).flatMap(raw -> {
            String body = raw == null ? "" : raw.trim();
            boolean looksXml = body.startsWith("<"); // 헤더가 틀려도 XML 감지

            try {
                if ((ct != null && ct.isCompatibleWith(MediaType.APPLICATION_XML)) || looksXml) {
                    return Mono.just(xmlMapper.readValue(body, bodyType));   // XML → DTO
                } else {
                    return Mono.just(objectMapper.readValue(body, bodyType)); // JSON → DTO
                }
            } catch (Exception first) {
                try { // 교차 fallback
                    if ((ct != null && ct.isCompatibleWith(MediaType.APPLICATION_XML)) || looksXml) {
                        return Mono.just(objectMapper.readValue(body, bodyType));
                    } else {
                        return Mono.just(xmlMapper.readValue(body, bodyType));
                    }
                } catch (Exception second) {
                    log.error("[{}] Failed to parse TourAPI response: {}", CLIENT_KEY, mask(body), second);
                    return Mono.error(new RuntimeException("Failed to parse TourAPI response", second));
                }
            }
        });
    }

    public static void qpIfPresent(UriBuilder b, String name, Object v) {
        if (v == null) return;
        if (v instanceof String s && s.isBlank()) return;
        b.queryParam(name, v);
    }

    private static String mask(String s) {
        if (s == null) return null;
        return s.replaceAll("(serviceKey=)([^&\\\"]+)", "$1****");
    }

    public static class Upstream4xxException extends RuntimeException {
        public final String body;
        public Upstream4xxException(String msg, String body) { super(msg); this.body = body; }
    }
    public static class Upstream5xxException extends RuntimeException {
        public final String body;
        public Upstream5xxException(String msg, String body) { super(msg); this.body = body; }
    }
}
