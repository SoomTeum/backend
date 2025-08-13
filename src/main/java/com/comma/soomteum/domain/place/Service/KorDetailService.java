package com.comma.soomteum.domain.place.Service;

import com.comma.soomteum.domain.place.Dto.TourApiRequestDto;
import com.comma.soomteum.domain.place.Dto.response.PlaceDetailResponseDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KorDetailService {

    private static final String PATH = "/detailCommon2";
    private final KorApiCaller caller;

    public Mono<PlaceDetailResponseDto> getDetail(TourApiRequestDto.DetailCommon2 req) {
        return caller.get(PATH, b -> {
                    b.queryParam("contentId", req.getContentId())
                            .queryParam("pageNo", req.pageNoOrDefault())
                            .queryParam("numOfRows", req.rowsOrDefault());
                }, DetailCommon2Api.class)
                .map(this::toDto);
    }

    private PlaceDetailResponseDto toDto(DetailCommon2Api api) {
        var body  = api != null && api.getResponse() != null ? api.getResponse().getBody() : null;
        var items = body != null ? body.getItems() : null;
        var list  = items != null ? items.getItem() : null;
        var it    = (list != null && !list.isEmpty()) ? list.get(0) : null;

        if (it == null) {
            return PlaceDetailResponseDto.builder().build();
        }

        return PlaceDetailResponseDto.from(
                it.getTitle(),
                it.getFirstimage(),
                it.getFirstimage2(),
                it.getMapx(),
                it.getMapy(),
                it.getAddr1(),
                it.getOverview()
        );
    }

    // 해당 기능 개발 후에 DTO 리팩토링 진행하겠습니다!
    /* ===== 외부(detailCommon2) 응답 파싱용 내부 DTO ===== */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetailCommon2Api {
        private Response response;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Response {
            private Header header;
            private Body body;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Header {
            private String resultCode;
            private String resultMsg;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Body {
            private Integer pageNo;
            private Integer numOfRows;
            private Integer totalCount;
            private Items items;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Items {
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            private List<Item> item;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Item {
            private String title;
            private String overview;
            private String firstimage;
            private String firstimage2;
            private String mapx;
            private String mapy;
            private String addr1;
        }
    }
}
