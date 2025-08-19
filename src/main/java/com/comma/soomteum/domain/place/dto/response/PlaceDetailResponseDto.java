package com.comma.soomteum.domain.place.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceDetailResponseDto {
    // DTO는 이 파일을 포함해 따로 리팩토링 진행하겠습니다!

    @Schema(description = "여행지 이름", example = "경포해수욕장")
    private String title;

    @Schema(description = "대표 이미지 URL", example = "http://tong.visitkorea.or.kr/cms/resource/58/2938658_image2_1.bmp")
    private String firstImage;

    @Schema(description = "경도(mapX)", example = "126.9883")
    private String mapX;

    @Schema(description = "위도(mapY)", example = "37.5512")
    private String mapY;

    @Schema(description = "주소", example = "강원특별자치도 강릉시 창해로 514 (안현동)")
    private String addr1;

    @Schema(description = "소개(개요)", example = "동해안 최대 해변으로 유명하며 강문동, 안현동에 있고...")
    private String overview;

    /**
     * 원본(detailCommon2) item -> 응답 DTO 매핑 헬퍼
     */
    public static PlaceDetailResponseDto from(
            String title,
            String firstimage,
            String firstimage2,
            String mapx,
            String mapy,
            String addr1,
            String overview
    ) {
        String img = (firstimage != null && !firstimage.isBlank())
                ? firstimage
                : firstimage2;
        return PlaceDetailResponseDto.builder()
                .title(title)
                .firstImage(img)
                .mapX(mapx)
                .mapY(mapy)
                .addr1(addr1)
                .overview(overview)
                .build();
    }
}
