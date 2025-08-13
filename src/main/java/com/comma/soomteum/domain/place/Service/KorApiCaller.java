package com.comma.soomteum.domain.place.Service;

import com.comma.soomteum.domain.place.Config.TourApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class KorApiCaller {

    private static final String CLIENT_KEY = "korService2";

    @Qualifier("tourApiClients")
    private final Map<String, WebClient> tourApiClients;

    private final TourApiProperties props;
    private final BiConsumer<UriBuilder, TourApiProperties> commonQueryApplier;
    private final Function<String, String> tourApiKeyResolver;

    public <T> Mono<T> get(String path, Consumer<UriBuilder> queryBuilder, Class<T> bodyType) {
        WebClient client = Objects.requireNonNull(
                tourApiClients.get(CLIENT_KEY), "WebClient not found for key: " + CLIENT_KEY);

        String serviceKey = tourApiKeyResolver.apply(CLIENT_KEY);

        return client.get()
                .uri(b -> {
                    b.path(path).queryParam("serviceKey", serviceKey);
                    queryBuilder.accept(b);
                    // 공통 파라미터(MobileOS, MobileApp, _type=json)
                    commonQueryApplier.accept(b, props);
                    return b.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(s -> s.is4xxClientError(), resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .doOnNext(body -> log.warn("[{}] 4xx: {}", CLIENT_KEY, mask(body)))
                                .flatMap(body -> Mono.error(new Upstream4xxException("TourAPI 4xx", body))))
                .onStatus(s -> s.is5xxServerError(), resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .doOnNext(body -> log.error("[{}] 5xx: {}", CLIENT_KEY, mask(body)))
                                .flatMap(body -> Mono.error(new Upstream5xxException("TourAPI 5xx", body))))
                .bodyToMono(bodyType)
                .doOnSubscribe(s -> log.debug(">> [{}] GET {}", CLIENT_KEY, path))
                .doOnError(WebClientResponseException.class,
                        ex -> log.error("[{}] HTTP error: {} {}", CLIENT_KEY, ex.getRawStatusCode(), ex.getResponseBodyAsString(), ex))
                .doOnError(ex -> log.error("[{}] Unexpected error", CLIENT_KEY, ex));
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
