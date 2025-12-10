package com.comma.soomteum.domain.place.service;

import com.comma.soomteum.domain.ai.dto.AiReviewRequest;
import com.comma.soomteum.domain.ai.dto.AiReviewResponse;
import com.comma.soomteum.domain.ai.service.AiRecommendationService;
import com.comma.soomteum.domain.ai.service.AiReviewService;
import com.comma.soomteum.domain.parking.dto.PublicParkingResponseDto;
import com.comma.soomteum.domain.parking.service.PublicParkingService;
import com.comma.soomteum.domain.external.tourapi.dto.CategoryCodeResponse;
import com.comma.soomteum.domain.external.tourapi.dto.KorService2Response;
import com.comma.soomteum.domain.external.tourapi.dto.TourApiRequestDto;
import com.comma.soomteum.domain.place.dto.response.PlaceDetailIntegratedResponseDto;
import com.comma.soomteum.domain.external.tourapi.dto.response.PlaceDetailResponseDto;
import com.comma.soomteum.domain.theme.entity.Theme;
import com.comma.soomteum.domain.theme.repository.ThemeRepository;
import com.comma.soomteum.domain.recommendation.service.TourService;
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
    private final KorCategoryService korCategoryService;
    private final TourService recommendPlacesService;
    private final UserPlaceService userPlaceService;
    private final AiReviewService aiReviewService;
    private final AiRecommendationService aiRecommendationService;
    private final PublicParkingService publicParkingService;
    private final ThemeRepository themeRepository;

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

        // 한적함 등급 및 테마 정보 조회 (비동기)
        return getTranquilityLevelAndTheme(contentId, item.getTitle(), item.getMapx(), item.getMapy())
                .flatMap(result -> {
                    builder.tranquilityLevel(result.getTranquilityLevel());
                    builder.theme(result.getTheme());
                    builder.themeName(result.getThemeName());

                    // 지역 정보 설정 및 강릉시 전용 기능 처리
                    return processRegionSpecificFeatures(builder, item, contentId);
                });
    }

    private Mono<TranquilityAndThemeResult> getTranquilityLevelAndTheme(String contentId, String title, String longitude, String latitude) {
        if (longitude == null || latitude == null) {
            return Mono.just(new TranquilityAndThemeResult(-1, "정보없음", "정보없음"));
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
                        log.info("locationPlaces에서 받은 item: title={}, contentId={}, cat1={}, cat2={}, cnctrRate={}",
                                item.getTitle(), item.getContentid(), item.getCat1(), item.getCat2(), item.getCnctrRate());

                        String cnctrRate = item.getCnctrRate();
                        int tranquilityLevel = calculateTranquilityLevel(cnctrRate);

                        // 테마로 cat2 중분류 코드 반환
                        String theme = (item.getCat2() != null && !item.getCat2().trim().isEmpty())
                            ? item.getCat2()
                            : (item.getCat1() != null ? item.getCat1() : "정보없음");

                        // cat2로 theme name 조회
                        String themeName = "정보없음";
                        if (item.getCat2() != null && !item.getCat2().trim().isEmpty()) {
                            try {
                                themeName = themeRepository.findByCat2(item.getCat2())
                                    .map(Theme::getName)
                                    .orElse("정보없음");
                            } catch (Exception e) {
                                log.warn("Theme 조회 실패: cat2={}", item.getCat2(), e);
                            }
                        }

                        log.info("최종 테마 설정: theme={}, themeName={}", theme, themeName);
                        return new TranquilityAndThemeResult(tranquilityLevel, theme, themeName);
                    })
                    .defaultIfEmpty(new TranquilityAndThemeResult(-1, "정보없음", "정보없음"));
        } catch (Exception e) {
            log.warn("한적함 등급 및 테마 정보 조회 실패: contentId={}", contentId, e);
            return Mono.just(new TranquilityAndThemeResult(-1, "정보없음", "정보없음"));
        }
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


        // 강릉시인 경우에만 AI 꿀팁과 주차장 정보 추가
        if (isGangneungRegion(region)) {
            return addGangneungSpecificFeatures(builder, item, contentId);
        }

        return Mono.just(builder.build());
    }

    private String extractRegionFromAddress(String address) {
        if (address == null) return "정보없음";

        if (address.contains("강남구")) return "강남구";
        if (address.contains("강동구")) return "강동구";
        if (address.contains("강북구")) return "강북구";
        if (address.contains("강서구")) return "강서구";
        if (address.contains("관악구")) return "관악구";
        if (address.contains("광진구")) return "광진구";
        if (address.contains("구로구")) return "구로구";
        if (address.contains("금천구")) return "금천구";
        if (address.contains("노원구")) return "노원구";
        if (address.contains("도봉구")) return "도봉구";
        if (address.contains("동대문구")) return "동대문구";
        if (address.contains("동작구")) return "동작구";
        if (address.contains("마포구")) return "마포구";
        if (address.contains("서대문구")) return "서대문구";
        if (address.contains("서초구")) return "서초구";
        if (address.contains("성동구")) return "성동구";
        if (address.contains("성북구")) return "성북구";
        if (address.contains("송파구")) return "송파구";
        if (address.contains("양천구")) return "양천구";
        if (address.contains("영등포구")) return "영등포구";
        if (address.contains("용산구")) return "용산구";
        if (address.contains("은평구")) return "은평구";
        if (address.contains("종로구")) return "종로구";
        if (address.contains("중구")) return "중구";
        if (address.contains("중랑구")) return "중랑구";

        if (address.contains("강화군")) return "강화군";
        if (address.contains("계양구")) return "계양구";
        if (address.contains("미추홀구")) return "미추홀구";
        if (address.contains("남동구")) return "남동구";
        if (address.contains("동구")) return "동구";
        if (address.contains("부평구")) return "부평구";
        if (address.contains("서구")) return "서구";
        if (address.contains("연수구")) return "연수구";
        if (address.contains("옹진군")) return "옹진군";
        if (address.contains("중구")) return "중구";

        if (address.contains("강릉시")) return "강릉시";
        if (address.contains("속초시")) return "속초시";
        if (address.contains("동해시")) return "동해시";
        if (address.contains("삼척시")) return "삼척시";
        if (address.contains("양양군")) return "양양군";
        if (address.contains("원주시")) return "원주시";
        if (address.contains("영월군")) return "영월군";
        if (address.contains("인제군")) return "인제군";
        if (address.contains("고성군")) return "고성군";
        if (address.contains("양구군")) return "양구군";

        // 부산광역시
        if (address.contains("강서구") && address.contains("부산")) return "부산 강서구";
        if (address.contains("금정구")) return "금정구";
        if (address.contains("기장군")) return "기장군";
        if (address.contains("남구") && address.contains("부산")) return "부산 남구";
        if (address.contains("동구") && address.contains("부산")) return "부산 동구";
        if (address.contains("동래구")) return "동래구";
        if (address.contains("부산진구")) return "부산진구";
        if (address.contains("북구") && address.contains("부산")) return "부산 북구";
        if (address.contains("사상구")) return "사상구";
        if (address.contains("사하구")) return "사하구";
        if (address.contains("서구") && address.contains("부산")) return "부산 서구";
        if (address.contains("수영구")) return "수영구";
        if (address.contains("연제구")) return "연제구";
        if (address.contains("영도구")) return "영도구";
        if (address.contains("중구") && address.contains("부산")) return "부산 중구";
        if (address.contains("해운대구")) return "해운대구";

        // 대구광역시
        if (address.contains("남구") && address.contains("대구")) return "대구 남구";
        if (address.contains("달서구")) return "달서구";
        if (address.contains("달성군")) return "달성군";
        if (address.contains("동구") && address.contains("대구")) return "대구 동구";
        if (address.contains("북구") && address.contains("대구")) return "대구 북구";
        if (address.contains("서구") && address.contains("대구")) return "대구 서구";
        if (address.contains("수성구")) return "수성구";
        if (address.contains("중구") && address.contains("대구")) return "대구 중구";

        // 광주광역시
        if (address.contains("광산구")) return "광산구";
        if (address.contains("남구") && address.contains("광주")) return "광주 남구";
        if (address.contains("동구") && address.contains("광주")) return "광주 동구";
        if (address.contains("북구") && address.contains("광주")) return "광주 북구";
        if (address.contains("서구") && address.contains("광주")) return "광주 서구";

        // 대전광역시
        if (address.contains("대덕구")) return "대덕구";
        if (address.contains("동구") && address.contains("대전")) return "대전 동구";
        if (address.contains("서구") && address.contains("대전")) return "대전 서구";
        if (address.contains("유성구")) return "유성구";
        if (address.contains("중구") && address.contains("대전")) return "대전 중구";

        // 울산광역시
        if (address.contains("남구") && address.contains("울산")) return "울산 남구";
        if (address.contains("동구") && address.contains("울산")) return "울산 동구";
        if (address.contains("북구") && address.contains("울산")) return "울산 북구";
        if (address.contains("중구") && address.contains("울산")) return "울산 중구";
        if (address.contains("울주군")) return "울주군";

        // 세종특별자치시
        if (address.contains("세종시") || address.contains("세종특별자치시")) return "세종시";

        // 경기도
        if (address.contains("가평군")) return "가평군";
        if (address.contains("고양시")) return "고양시";
        if (address.contains("과천시")) return "과천시";
        if (address.contains("광명시")) return "광명시";
        if (address.contains("광주시") && address.contains("경기")) return "경기 광주시";
        if (address.contains("구리시")) return "구리시";
        if (address.contains("군포시")) return "군포시";
        if (address.contains("김포시")) return "김포시";
        if (address.contains("남양주시")) return "남양주시";
        if (address.contains("동두천시")) return "동두천시";
        if (address.contains("부천시")) return "부천시";
        if (address.contains("성남시")) return "성남시";
        if (address.contains("수원시")) return "수원시";
        if (address.contains("시흥시")) return "시흥시";
        if (address.contains("안산시")) return "안산시";
        if (address.contains("안성시")) return "안성시";
        if (address.contains("안양시")) return "안양시";
        if (address.contains("양주시")) return "양주시";
        if (address.contains("양평군")) return "양평군";
        if (address.contains("여주시")) return "여주시";
        if (address.contains("연천군")) return "연천군";
        if (address.contains("오산시")) return "오산시";
        if (address.contains("용인시")) return "용인시";
        if (address.contains("의왕시")) return "의왕시";
        if (address.contains("의정부시")) return "의정부시";
        if (address.contains("이천시")) return "이천시";
        if (address.contains("파주시")) return "파주시";
        if (address.contains("평택시")) return "평택시";
        if (address.contains("포천시")) return "포천시";
        if (address.contains("하남시")) return "하남시";
        if (address.contains("화성시")) return "화성시";

        // 강원도 추가 지역
        if (address.contains("춘천시")) return "춘천시";
        if (address.contains("원주시")) return "원주시";
        if (address.contains("태백시")) return "태백시";
        if (address.contains("홍천군")) return "홍천군";
        if (address.contains("횡성군")) return "횡성군";
        if (address.contains("평창군")) return "평창군";
        if (address.contains("정선군")) return "정선군";
        if (address.contains("철원군")) return "철원군";
        if (address.contains("화천군")) return "화천군";

        // 충청북도
        if (address.contains("청주시")) return "청주시";
        if (address.contains("충주시")) return "충주시";
        if (address.contains("제천시")) return "제천시";
        if (address.contains("보은군")) return "보은군";
        if (address.contains("옥천군")) return "옥천군";
        if (address.contains("영동군")) return "영동군";
        if (address.contains("증평군")) return "증평군";
        if (address.contains("진천군")) return "진천군";
        if (address.contains("괴산군")) return "괴산군";
        if (address.contains("음성군")) return "음성군";
        if (address.contains("단양군")) return "단양군";

        // 충청남도
        if (address.contains("천안시")) return "천안시";
        if (address.contains("공주시")) return "공주시";
        if (address.contains("보령시")) return "보령시";
        if (address.contains("아산시")) return "아산시";
        if (address.contains("서산시")) return "서산시";
        if (address.contains("논산시")) return "논산시";
        if (address.contains("계룡시")) return "계룡시";
        if (address.contains("당진시")) return "당진시";
        if (address.contains("금산군")) return "금산군";
        if (address.contains("부여군")) return "부여군";
        if (address.contains("서천군")) return "서천군";
        if (address.contains("청양군")) return "청양군";
        if (address.contains("홍성군")) return "홍성군";
        if (address.contains("예산군")) return "예산군";
        if (address.contains("태안군")) return "태안군";

        // 전라북도
        if (address.contains("전주시")) return "전주시";
        if (address.contains("군산시")) return "군산시";
        if (address.contains("익산시")) return "익산시";
        if (address.contains("정읍시")) return "정읍시";
        if (address.contains("남원시")) return "남원시";
        if (address.contains("김제시")) return "김제시";
        if (address.contains("완주군")) return "완주군";
        if (address.contains("진안군")) return "진안군";
        if (address.contains("무주군")) return "무주군";
        if (address.contains("장수군")) return "장수군";
        if (address.contains("임실군")) return "임실군";
        if (address.contains("순창군")) return "순창군";
        if (address.contains("고창군")) return "고창군";
        if (address.contains("부안군")) return "부안군";

        // 전라남도
        if (address.contains("목포시")) return "목포시";
        if (address.contains("여수시")) return "여수시";
        if (address.contains("순천시")) return "순천시";
        if (address.contains("나주시")) return "나주시";
        if (address.contains("광양시")) return "광양시";
        if (address.contains("담양군")) return "담양군";
        if (address.contains("곡성군")) return "곡성군";
        if (address.contains("구례군")) return "구례군";
        if (address.contains("고흥군")) return "고흥군";
        if (address.contains("보성군")) return "보성군";
        if (address.contains("화순군")) return "화순군";
        if (address.contains("장흥군")) return "장흥군";
        if (address.contains("강진군")) return "강진군";
        if (address.contains("해남군")) return "해남군";
        if (address.contains("영암군")) return "영암군";
        if (address.contains("무안군")) return "무안군";
        if (address.contains("함평군")) return "함평군";
        if (address.contains("영광군")) return "영광군";
        if (address.contains("장성군")) return "장성군";
        if (address.contains("완도군")) return "완도군";
        if (address.contains("진도군")) return "진도군";
        if (address.contains("신안군")) return "신안군";

        // 경상북도
        if (address.contains("포항시")) return "포항시";
        if (address.contains("경주시")) return "경주시";
        if (address.contains("김천시")) return "김천시";
        if (address.contains("안동시")) return "안동시";
        if (address.contains("구미시")) return "구미시";
        if (address.contains("영주시")) return "영주시";
        if (address.contains("영천시")) return "영천시";
        if (address.contains("상주시")) return "상주시";
        if (address.contains("문경시")) return "문경시";
        if (address.contains("경산시")) return "경산시";
        if (address.contains("군위군")) return "군위군";
        if (address.contains("의성군")) return "의성군";
        if (address.contains("청송군")) return "청송군";
        if (address.contains("영양군")) return "영양군";
        if (address.contains("영덕군")) return "영덕군";
        if (address.contains("청도군")) return "청도군";
        if (address.contains("고령군")) return "고령군";
        if (address.contains("성주군")) return "성주군";
        if (address.contains("칠곡군")) return "칠곡군";
        if (address.contains("예천군")) return "예천군";
        if (address.contains("봉화군")) return "봉화군";
        if (address.contains("울진군")) return "울진군";
        if (address.contains("울릉군")) return "울릉군";

        // 경상남도
        if (address.contains("창원시")) return "창원시";
        if (address.contains("진주시")) return "진주시";
        if (address.contains("통영시")) return "통영시";
        if (address.contains("사천시")) return "사천시";
        if (address.contains("김해시")) return "김해시";
        if (address.contains("밀양시")) return "밀양시";
        if (address.contains("거제시")) return "거제시";
        if (address.contains("양산시")) return "양산시";
        if (address.contains("의령군")) return "의령군";
        if (address.contains("함안군")) return "함안군";
        if (address.contains("창녕군")) return "창녕군";
        if (address.contains("고성군") && address.contains("경남")) return "경남 고성군";
        if (address.contains("남해군")) return "남해군";
        if (address.contains("하동군")) return "하동군";
        if (address.contains("산청군")) return "산청군";
        if (address.contains("함양군")) return "함양군";
        if (address.contains("거창군")) return "거창군";
        if (address.contains("합천군")) return "합천군";

        // 제주특별자치도
        if (address.contains("제주시")) return "제주시";
        if (address.contains("서귀포시")) return "서귀포시";

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

    private String determineThemeFromCategories(String cat1, String cat2) {
        log.info("카테고리 정보 확인: cat1={}, cat2={}", cat1, cat2);

        if (cat1 == null && cat2 == null) {
            log.warn("cat1과 cat2 모두 null");
            return "정보없음";
        }

        try {
            // cat2(중분류)가 있으면 중분류 이름을 가져오기
            if (cat2 != null && !cat2.trim().isEmpty()) {
                log.info("cat2로 카테고리 조회: cat1={}, cat2={}", cat1, cat2);
                // 특정 cat2의 이름을 얻으려면 cat1과 함께 조회해야 할 수 있음
                String result = getCategoryNameByCode(cat1, cat2).block();
                log.info("cat2 조회 결과: {}", result);
                return result;
            }

            // cat1(대분류)만 있는 경우
            if (cat1 != null && !cat1.trim().isEmpty()) {
                log.info("cat1로 카테고리 조회: {}", cat1);
                String result = getCategoryNameByCode(cat1, null).block();
                log.info("cat1 조회 결과: {}", result);
                return result;
            }

            log.warn("cat1, cat2 모두 빈 문자열");
            return "정보없음";
        } catch (Exception e) {
            log.warn("카테고리 명칭 조회 실패: cat1={}, cat2={}", cat1, cat2, e);
            return "정보없음";
        }
    }

    private Mono<String> getCategoryNameByCode(String cat1, String cat2) {
        return korCategoryService.getCategoryCode(
                TourApiRequestDto.CategoryCode.builder()
                        .cat1(cat1)
                        .cat2(cat2)
                        .build())
                .map(response -> {
                    log.info("카테고리 API 응답: {}", response);
                    if (response.getBody() != null &&
                        response.getBody().getItems() != null &&
                        response.getBody().getItems().getItem() != null &&
                        !response.getBody().getItems().getItem().isEmpty()) {

                        // cat2가 있는 경우: cat2에 해당하는 항목 찾기
                        if (cat2 != null && !cat2.trim().isEmpty()) {
                            for (CategoryCodeResponse.Item item : response.getBody().getItems().getItem()) {
                                if (cat2.equals(item.getCode())) {
                                    log.info("cat2 매칭됨: code={}, name={}", item.getCode(), item.getName());
                                    return item.getName();
                                }
                            }
                        }

                        // cat1만 있는 경우: cat1에 해당하는 항목 찾기
                        if (cat1 != null && !cat1.trim().isEmpty()) {
                            for (CategoryCodeResponse.Item item : response.getBody().getItems().getItem()) {
                                if (cat1.equals(item.getCode())) {
                                    log.info("cat1 매칭됨: code={}, name={}", item.getCode(), item.getName());
                                    return item.getName();
                                }
                            }
                        }

                        log.warn("매칭되는 카테고리 코드 없음: cat1={}, cat2={}", cat1, cat2);
                    }
                    return "정보없음";
                })
                .onErrorReturn("정보없음");
    }

    private static class TranquilityAndThemeResult {
        private final int tranquilityLevel;
        private final String theme;
        private final String themeName;

        public TranquilityAndThemeResult(int tranquilityLevel, String theme, String themeName) {
            this.tranquilityLevel = tranquilityLevel;
            this.theme = theme;
            this.themeName = themeName;
        }

        public int getTranquilityLevel() {
            return tranquilityLevel;
        }

        public String getTheme() {
            return theme;
        }

        public String getThemeName() {
            return themeName;
        }
    }
}
