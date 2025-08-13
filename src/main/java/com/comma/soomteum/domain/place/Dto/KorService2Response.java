package com.comma.soomteum.domain.place.Dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KorService2Response {
    private Response response;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }


    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Body {
        private Items items;
        private Integer totalCount;
        private Integer pageNo;
        private Integer numOfRows;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Items {
        private List<locationBasedListResponseDto> item;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class locationBasedListResponseDto {
        private String title;
        private String contentid;
        private String cat1;
        private String cat2;
        private String firstimage;
        private String dist;
    }
}
