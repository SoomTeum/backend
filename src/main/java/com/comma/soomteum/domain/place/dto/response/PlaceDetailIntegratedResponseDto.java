package com.comma.soomteum.domain.place.dto.response;

import com.comma.soomteum.domain.parking.dto.PublicParkingResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "여행지 상세 통합 정보 응답")
public class PlaceDetailIntegratedResponseDto {

    @Schema(description = "여행지 이름", example = "경포해수욕장")
    private String placeName;

    @Schema(description = "여행지 대표 사진 URL", example = "http://tong.visitkorea.or.kr/cms/resource/58/2938658_image2_1.bmp")
    private String placeImageUrl;

    @Schema(description = "여행지 주소", example = "강원특별자치도 강릉시 창해로 514 (안현동)")
    private String placeAddress;

    @Schema(description = "여행지 지역", example = "강릉시")
    private String region;

    @Schema(description = "테마 분류", example = "자연관광지")
    private String theme;

    @Schema(description = "한적함 등급 (1-5단계, -1: 데이터 없음)", example = "3")
    private Integer tranquilityLevel;

    @Schema(description = "좋아요 수", example = "127")
    private Long likeCount;

    @Schema(description = "여행지 소개", example = "동해안 최대 해변으로 유명하며...")
    private String introduction;

    @Schema(description = "AI 꿀팁 요약 (강릉시만 제공)")
    private String aiTipSummary;

    @Schema(description = "근처 공영주차장 목록 (강릉시만 제공)")
    private List<PublicParkingResponseDto> nearbyParkingLots;

    @Schema(description = "경도", example = "128.8761")
    private String longitude;

    @Schema(description = "위도", example = "37.7519")
    private String latitude;
}