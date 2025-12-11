package com.comma.soomteum.domain.external.tourapi.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")   // XML <response>...</response>
@JsonRootName("response")                        // JSON { "response": { ... } }
public class PlaceDetailResponseDto {

    private Header header;
    private Body body;

    // ===================== 헤더/바디 구조 =====================
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
        private Integer totalCount;
        private Integer pageNo;
        private Integer numOfRows;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<Item> item;
    }

    // ===================== 실제 여행지 아이템 =====================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @Schema(description = "여행지 이름", example = "경포해수욕장")
        private String title;

        @Schema(description = "대표 이미지 URL", example = "http://tong.visitkorea.or.kr/cms/resource/58/2938658_image2_1.bmp")
        private String firstimage;

        @Schema(description = "보조 이미지 URL")
        private String firstimage2;

        @Schema(description = "경도(mapX)", example = "126.9883")
        private String mapx;

        @Schema(description = "위도(mapY)", example = "37.5512")
        private String mapy;

        @Schema(description = "주소", example = "강원특별자치도 강릉시 창해로 514 (안현동)")
        private String addr1;

        @Schema(description = "소개(개요)", example = "동해안 최대 해변으로 유명하며 강문동, 안현동에 있고...")
        private String overview;
    }
}
