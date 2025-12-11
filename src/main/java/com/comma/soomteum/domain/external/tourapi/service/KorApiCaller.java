package com.comma.soomteum.domain.external.tourapi.service;

import com.comma.soomteum.domain.external.tourapi.config.TourApiProperties;
import com.comma.soomteum.domain.external.tourapi.dto.TourApiErrorResponse;
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

import java.net.URI;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
        objectMapper.findAndRegisterModules();
    }

    private final XmlMapper xmlMapper = new XmlMapper();
    {
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        xmlMapper.findAndRegisterModules();
    }

    public <T> Mono<T> get(String path, Consumer<UriBuilder> queryBuilder, Class<T> bodyType) {
        WebClient client = Objects.requireNonNull(
                tourApiClients.get(CLIENT_KEY), "WebClient not found for key: " + CLIENT_KEY);

        // 입력은 raw(디코딩) 키
        String rawServiceKey = tourApiKeyResolver.apply(CLIENT_KEY);

        return client.get()
                .uri(b -> {
                    b.path(path);
                    queryBuilder.accept(b);              // 개별 쿼리
                    commonQueryApplier.accept(b, props); // MobileOS/MobileApp/_type

                    // 1) 우선 serviceKey 없이 URI를 만든다
                    URI built = b.build();

                    // 2) serviceKey만 직접 퍼센트 인코딩(+ → %2B, / → %2F, = → %3D)
                    String encodedKey = java.net.URLEncoder.encode(
                            rawServiceKey, java.nio.charset.StandardCharsets.UTF_8
                    );

                    // 3) 최종 URL 문자열에 serviceKey를 붙여서 반환 (재인코딩 방지)
                    String sep = (built.getQuery() == null || built.getQuery().isEmpty()) ? "?" : "&";
                    String finalUrl = built.toString() + sep + "serviceKey=" + encodedKey;
                    return java.net.URI.create(finalUrl);
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
            boolean looksXml = body.startsWith("<");

            // OpenAPI 에러(XML/JSON) 우선 파싱
            try {
                TourApiErrorResponse err = looksXml
                        ? xmlMapper.readValue(body, TourApiErrorResponse.class)
                        : objectMapper.readValue(body, TourApiErrorResponse.class);

                // 편의 getter가 있다면 사용 (getErrorCode/getErrorMsg)
                String code = null, msg = null;
                if (err != null) {
                    try {
                        code = err.getErrorCode();
                        msg  = err.getErrorMsg();
                    } catch (NoSuchMethodError | Exception ignore) {
                        // 구버전 DTO일 경우 직접 header에서 추출
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

            // 본문 파싱 (XML/JSON 모두 지원)
            try {
                if ((ct != null && ct.isCompatibleWith(MediaType.APPLICATION_XML)) || looksXml) {
                    return Mono.just(xmlMapper.readValue(body, bodyType));
                } else {
                    return Mono.just(objectMapper.readValue(body, bodyType));
                }
            } catch (Exception first) {
                try {
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
