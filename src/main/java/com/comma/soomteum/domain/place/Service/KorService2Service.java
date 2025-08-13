package com.comma.soomteum.domain.place.Service;

import com.comma.soomteum.domain.place.Config.TourApiProperties;
import com.comma.soomteum.domain.place.Dto.KorService2Response;
import com.comma.soomteum.domain.place.Dto.TourApiRequestDto;
import lombok.RequiredArgsConstructor;
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
    private final BiConsumer<UriBuilder, TourApiProperties> commonQueryApplier;

    public Mono<KorService2Response> locationBasedList(TourApiRequestDto.LocationBasedList2 req) {
        // 1) 사용할 클라이언트 선택
        final String clientKey = "korService2";
        final WebClient client = Objects.requireNonNull(
                tourApiClients.get(clientKey),
                "WebClient not found for key: " + clientKey
        );

        // 2) 서비스키 결정
        final String serviceKey = props.getClients().get(clientKey).getServiceKey();

        // 3) 호출
        return client.get()
                .uri(b -> {
                    b.path("/locationBasedList2")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("mapX", req.getMapX())
                            .queryParam("mapY", req.getMapY())
                            .queryParam("radius", req.getRadius())
                            .queryParam("pageNo", req.getPageNo())
                            .queryParam("numOfRows", req.getNumOfRows());
                    if (req.getArrange() != null) {
                        b.queryParam("arrange", req.getArrange());
                    }
                    commonQueryApplier.accept(b, props); // MobileOS/MobileApp/_type=json
                    return b.build();
                })
                .retrieve()
                .bodyToMono(KorService2Response.class);
    }
}
