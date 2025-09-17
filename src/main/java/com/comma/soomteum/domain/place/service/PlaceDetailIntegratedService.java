package com.comma.soomteum.domain.place.service;

import com.comma.soomteum.domain.ai.dto.AiReviewRequest;
import com.comma.soomteum.domain.ai.dto.AiReviewResponse;
import com.comma.soomteum.domain.ai.service.AiRecommendationService;
import com.comma.soomteum.domain.ai.service.AiReviewService;
import com.comma.soomteum.domain.parking.dto.PublicParkingResponseDto;
import com.comma.soomteum.domain.parking.service.PublicParkingService;
import com.comma.soomteum.domain.place.dto.KorService2Response;
import com.comma.soomteum.domain.place.dto.TourApiRequestDto;
import com.comma.soomteum.domain.place.dto.response.PlaceDetailIntegratedResponseDto;
import com.comma.soomteum.domain.place.dto.response.PlaceDetailResponseDto;
import com.comma.soomteum.domain.tour.service.TourService;
import com.comma.soomteum.domain.userPlace.enums.UserActionType;
import com.comma.soomteum.domain.userPlace.service.UserPlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceDetailIntegratedService {

    private final KorDetailService korDetailService;
    private final TourService recommendPlacesService;
    private final UserPlaceService userPlaceService;
    private final AiReviewService aiReviewService;
    private final AiRecommendationService aiRecommendationService;
    private final PublicParkingService publicParkingService;

    private static final String GANGNEUNG_REGION_CODE = "32230";
    private static final String GANGNEUNG_AREA_CODE = "32";
    private static final int DEFAULT_PARKING_LIMIT = 5;

    public Mono<PlaceDetailIntegratedResponseDto> getIntegratedPlaceDetail(String contentId) {
        return korDetailService.getDetail(
                TourApiRequestDto.DetailCommon2.builder()
                        .contentId(contentId)
                        .build())
                .flatMap(placeDetail -> {
                    if (placeDetail.getBody() == null || 
                        placeDetail.getBody().getItems() == null || 
                        placeDetail.getBody().getItems().getItem() == null ||
                        placeDetail.getBody().getItems().getItem().isEmpty()) {
                        return Mono.error(new RuntimeException("여행지 정보를 찾을 수 없습니다."));
                    }

                    PlaceDetailResponseDto.Item item = placeDetail.getBody().getItems().getItem().get(0);
                    
                    return buildIntegratedResponse(contentId, item);
                });
    }

    private Mono<PlaceDetailIntegratedResponseDto> buildIntegratedResponse(String contentId, PlaceDetailResponseDto.Item item) {
        PlaceDetailIntegratedResponseDto.PlaceDetailIntegratedResponseDtoBuilder builder = 
                PlaceDetailIntegratedResponseDto.builder()
                        .placeName(item.getTitle())
                        .placeImageUrl(item.getFirstimage())
                        .placeAddress(item.getAddr1())
                        .introduction(item.getOverview())
                        .longitude(item.getMapx())
                        .latitude(item.getMapy());

        // 좋아요 수 조회
        try {
            long likeCount = userPlaceService.getActionCountByContentId(contentId, UserActionType.LIKE);
            builder.likeCount(likeCount);
        } catch (Exception e) {
            log.warn("좋아요 수 조회 실패: contentId={}", contentId, e);
            builder.likeCount(0L);
        }

        // 한적함 등급 조회 (비동기)
        return getTranquilityLevel(contentId, item.getTitle(), item.getMapx(), item.getMapy())
                .flatMap(tranquilityLevel -> {
                    builder.tranquilityLevel(tranquilityLevel);
                    
                    // 지역 정보 설정 및 강릉시 전용 기능 처리
                    return processRegionSpecificFeatures(builder, item, contentId);
                });
    }

    private Mono<Integer> getTranquilityLevel(String contentId, String title, String longitude, String latitude) {
        if (longitude == null || latitude == null) {
            return Mono.just(-1);
        }

        try {
            return recommendPlacesService.locationPlaces(
                    TourApiRequestDto.LocationBasedList2.builder()
                            .mapX(Double.parseDouble(longitude))
                            .mapY(Double.parseDouble(latitude))
                            .radius(1000)
                            .build())
                    .filter(item -> contentId.equals(item.getContentid()))
                    .next()
                    .map(item -> {
                        String cnctrRate = item.getCnctrRate();
                        return calculateTranquilityLevel(cnctrRate);
                    })
                    .defaultIfEmpty(-1);
        } catch (Exception e) {
            log.warn("한적함 등급 조회 실패: contentId={}", contentId, e);
            return Mono.just(-1);
        }
    }

    private Integer calculateTranquilityLevel(String cnctrRate) {
        try {
            double rateValue = (cnctrRate != null) ? Double.parseDouble(cnctrRate) : -1.0;
            return determineTranquilityLevel(rateValue);
        } catch (NumberFormatException e) {
            log.warn("한적함 등급 파싱 실패: {}", cnctrRate);
            return -1;
        }
    }

    private int determineTranquilityLevel(double rateValue) {
        if (rateValue < 0) {
            return -1;
        }
        if (rateValue <= 20.0) {
            return 1;
        } else if (rateValue <= 40.0) {
            return 2;
        } else if (rateValue <= 60.0) {
            return 3;
        } else if (rateValue <= 80.0) {
            return 4;
        } else {
            return 5;
        }
    }

    private Mono<PlaceDetailIntegratedResponseDto> processRegionSpecificFeatures(
            PlaceDetailIntegratedResponseDto.PlaceDetailIntegratedResponseDtoBuilder builder,
            PlaceDetailResponseDto.Item item, String contentId) {
        
        // 지역 정보 추출 (주소에서)
        String region = extractRegionFromAddress(item.getAddr1());
        builder.region(region);

        // 테마 정보는 별도 서비스에서 조회해야 함 (현재는 기본값 설정)
        builder.theme("관광지");

        // 강릉시인 경우에만 AI 꿀팁과 주차장 정보 추가
        if (isGangneungRegion(region)) {
            return addGangneungSpecificFeatures(builder, item, contentId);
        }

        return Mono.just(builder.build());
    }

    private String extractRegionFromAddress(String address) {
        if (address == null) return "정보없음";
        
        if (address.contains("강릉시")) return "강릉시";
        if (address.contains("속초시")) return "속초시";
        if (address.contains("동해시")) return "동해시";
        // 추가 지역 매핑 필요시 여기에 추가
        
        return "기타";
    }

    private boolean isGangneungRegion(String region) {
        return "강릉시".equals(region);
    }

    private Mono<PlaceDetailIntegratedResponseDto> addGangneungSpecificFeatures(
            PlaceDetailIntegratedResponseDto.PlaceDetailIntegratedResponseDtoBuilder builder,
            PlaceDetailResponseDto.Item item, String contentId) {
        
        // AI 꿀팁 조회
        String aiTipSummary = getAiTipSummary(item.getTitle());
        builder.aiTipSummary(aiTipSummary);

        // 주변 주차장 정보 조회
        List<PublicParkingResponseDto> nearbyParking = getNearbyParking(item.getMapx(), item.getMapy());
        builder.nearbyParkingLots(nearbyParking);

        return Mono.just(builder.build());
    }

    private String getAiTipSummary(String placeName) {
        if (placeName == null || placeName.trim().isEmpty()) {
            return null;
        }

        try {
            AiReviewRequest request = new AiReviewRequest(placeName);
            AiReviewResponse response = aiReviewService.summarizeByPlaceName(request);
            return response.getSummaryText();
        } catch (Exception e) {
            log.warn("AI 꿀팁 조회 실패: placeName={}", placeName, e);
            return null;
        }
    }

    private List<PublicParkingResponseDto> getNearbyParking(String longitude, String latitude) {
        if (longitude == null || latitude == null) {
            return null;
        }

        try {
            BigDecimal lat = new BigDecimal(latitude);
            BigDecimal lon = new BigDecimal(longitude);
            return publicParkingService.findNearbyParking(lat, lon, GANGNEUNG_REGION_CODE, DEFAULT_PARKING_LIMIT);
        } catch (Exception e) {
            log.warn("주변 주차장 조회 실패: longitude={}, latitude={}", longitude, latitude, e);
            return null;
        }
    }
}