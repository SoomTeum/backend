package com.comma.soomteum.domain.place.Service;

import com.comma.soomteum.domain.place.Config.TourApiProperties;
import com.comma.soomteum.domain.place.Dto.KorService2Response;
import com.comma.soomteum.domain.place.Dto.TourApiRequestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiConsumer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

@Service
@RequiredArgsConstructor
public class KorService2Service {

    @Qualifier("tourApiClients")
    private final Map<String, WebClient> tourApiClients;

    private final TourApiProperties props;

    public Mono<KorService2Response> locationBasedList(TourApiRequestDto.LocationBasedList2 req) {
        final String clientKey = "korService2";
        final WebClient client = Objects.requireNonNull(
                tourApiClients.get(clientKey), "WebClient not found for key: " + clientKey);

        final String serviceKey = props.getClients().get(clientKey).getServiceKey();

        WebClient.RequestHeadersSpec<?> spec = client.get()
                .uri(b -> {
                    b.path("/locationBasedList2")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("mapX", req.getMapX())
                            .queryParam("mapY", req.getMapY())
                            .queryParam("radius", req.getRadius())
                            .queryParam("pageNo", req.pageNoOrDefault())
                            .queryParam("numOfRows", req.rowsOrDefault())
                            .queryParam("arrange", req.arrangeOrDefault());

                    if (req.getCat1() != null) b.queryParam("cat1", req.getCat1());
                    if (req.getCat2() != null) b.queryParam("cat2", req.getCat2());

                    // JSON 강제 파라미터
                    b.queryParam("MobileOS", "ETC")
                            .queryParam("MobileApp", "soomteum")
                            .queryParam("_type", "json");

                    return b.build();
                })
                .accept(MediaType.APPLICATION_JSON);

        ObjectMapper json = new ObjectMapper();

        return spec.retrieve()
                .bodyToMono(String.class)
                .map(body -> {
                    String s = body == null ? "" : body.trim();
                    if (s.startsWith("{")) {
                        try {
                            return new ObjectMapper().readValue(s, KorService2Response.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    // JSON이 아니면 에러 본문을 충분히 로그로 남김
                    int max = Math.min(2000, s.length());
                    System.err.println("<< BODY(head)=" + s.substring(0, max));
                    throw new IllegalStateException("Unexpected response (not JSON). See server logs for BODY.");
                });

    }


}
