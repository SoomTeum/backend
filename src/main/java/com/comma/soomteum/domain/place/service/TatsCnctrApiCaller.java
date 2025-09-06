package com.comma.soomteum.domain.place.service;

import com.comma.soomteum.domain.place.config.TourApiProperties;
import com.comma.soomteum.domain.place.dto.TourApiErrorResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.annotation.PostConstruct;
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

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
@Component
@Slf4j
public class TatsCnctrApiCaller {

    private static final String CLIENT_KEY = "TatsCnctrRateService";
    private static final String SERVICE_KEY_OWNER = "korService2";

    @Qualifier("tourApiClients")
    private final Map<String, WebClient> tourApiClients;

    private final TourApiProperties props;
    private final BiConsumer<UriBuilder, TourApiProperties> commonQueryApplier;
    private final Function<String, String> tourApiKeyResolver;

    private final ObjectMapper objectMapper = new ObjectMapper(); // 별도 ObjectMapper 사용

    @PostConstruct
    void setupMappers() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.findAndRegisterModules();

        objectMapper
                .coercionConfigFor(LogicalType.POJO)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
    }

    /**
     * TatsCnctrRateService GET 호출 - JSON 고정
     */
    public <T> Mono<T> get(String path, Consumer<UriBuilder> queryBuilder, Class<T> bodyType) {
        WebClient client = Objects.requireNonNull(
                tourApiClients.get(CLIENT_KEY), "WebClient not found for key: " + CLIENT_KEY);

        String rawServiceKey = tourApiKeyResolver.apply(SERVICE_KEY_OWNER);

        return client.get()
                .uri(b -> {
                    b.path(path);
                    queryBuilder.accept(b);

                    // 공통 파라미터 적용
                    commonQueryApplier.accept(b, props);

                    // JSON 강제 (_type=json 없을 가능성 대비해서 항상 세팅)
                    b.queryParam("_type", "json");

                    URI built = b.build();

                    String encodedKey = java.net.URLEncoder.encode(
                            rawServiceKey, java.nio.charset.StandardCharsets.UTF_8);
                    String sep = (built.getQuery() == null || built.getQuery().isEmpty()) ? "?" : "&";
                    String finalUrl = built + sep + "serviceKey=" + encodedKey;
                    return java.net.URI.create(finalUrl);
                })
                // JSON만 허용
                .accept(MediaType.APPLICATION_JSON)
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

        // 본문 문자열로 받고 JSON으로만 파싱
        return resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(String::trim)
                .flatMap(body -> {
                    // (선택) 서버가 XML/텍스트로 잘못 내려주면 경고
                    resp.headers().contentType().ifPresent(ct -> {
                        if (!MediaType.APPLICATION_JSON.includes(ct)) {
                            log.warn("[{}] Non-JSON Content-Type reported: {} (forcing JSON parse)", CLIENT_KEY, ct);
                        }
                    });

                    // 1) Upstream 에러 포맷(JSON) 시도
                    try {
                        TourApiErrorResponse err = objectMapper.readValue(body, TourApiErrorResponse.class);
                        String code = null, msg = null;
                        if (err != null) {
                            try {
                                code = err.getErrorCode();
                                msg  = err.getErrorMsg();
                            } catch (Exception ignore) {
                                if (err.getCmmMsgHeader() != null) {
                                    code = err.getCmmMsgHeader().getReturnReasonCode();
                                    msg  = err.getCmmMsgHeader().getErrMsg();
                                    if (msg == null || msg.isBlank()) {
                                        msg = err.getCmmMsgHeader().getReturnAuthMsg();
                                    }
                                }
                            }
                        }
                        if (code != null && !code.isBlank()) {
                            log.error("[{}] Upstream ERROR: {} ({})", CLIENT_KEY, msg, code);
                            return Mono.error(new RuntimeException(
                                    "TourAPI ERROR: " + (msg == null ? "UNKNOWN" : msg) + " [" + code + "]"));
                        }
                    } catch (Exception ignore) {
                        // 정상 데이터일 수 있으니 무시
                    }

                    // 2) 정상 데이터(JSON) 파싱
                    try {
                        return Mono.just(objectMapper.readValue(body, bodyType));
                    } catch (Exception e) {
                        log.error("[{}] Failed to parse TourAPI JSON response: {}", CLIENT_KEY, mask(body), e);
                        return Mono.error(new RuntimeException("Failed to parse TourAPI JSON response", e));
                    }
                });
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
