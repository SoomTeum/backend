package com.comma.soomteum.domain.place.service;

import com.comma.soomteum.config.CacheConfig;
import com.comma.soomteum.domain.parking.dto.PublicParkingResponseDto;
import com.comma.soomteum.domain.parking.service.PublicParkingService;
import com.comma.soomteum.domain.place.dto.response.PlaceDetailWithParkingDto;
import com.comma.soomteum.domain.place.entity.Place;
import com.comma.soomteum.domain.place.repository.PlaceRepository;
import com.comma.soomteum.domain.region.repository.RegionRepository;
import com.comma.soomteum.domain.theme.repository.ThemeRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PublicParkingService publicParkingService;
    private final RegionRepository regionRepository;
    private final ThemeRepository themeRepository;

    public Place findPlaceById(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));
    }

    public Place findPlaceByContentId(String contentId) {
        return placeRepository.findByContentId(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));
    }

    public Optional<Place> findByContentId(String contentId) {
        return placeRepository.findByContentId(contentId);
    }

    /**
     * contentId로 좋아요 수 조회 (캐시 적용)
     *
     * 캐시 키: contentId
     * TTL: 10분
     *
     * @return 좋아요 수 또는 null (장소가 없는 경우)
     */
    @Cacheable(
            cacheNames = CacheConfig.PLACE_LIKE_CACHE,
            key = "#contentId"
    )
    public Long getLikeCount(String contentId) {
        log.debug("[PlaceService] 좋아요 수 조회: contentId={}", contentId);
        return placeRepository.findByContentId(contentId)
                .map(Place::getLikeCount)
                .orElse(null);
    }

    public PlaceDetailWithParkingDto getPlaceDetailWithParking(Long placeId) {
        Place place = findPlaceById(placeId);
        
        List<PublicParkingResponseDto> nearbyParkingLots = List.of();
        
        if (place.getLatitude() != null && place.getLongitude() != null && 
            place.getRegion() != null && place.getRegion().getKorAreaCode() != null) {
            
            String regionCode = place.getRegion().getKorAreaCode();
            if ("32".equals(regionCode)) {
                nearbyParkingLots = publicParkingService.findNearbyParking(
                    place.getLatitude(), 
                    place.getLongitude(), 
                    "32230", 
                    5
                );
            }
        }
        
        return PlaceDetailWithParkingDto.from(place, nearbyParkingLots);
    }

    public PlaceDetailWithParkingDto getPlaceDetailWithNearestParking(String contentId) {
        Place place = findPlaceByContentId(contentId);
        
        List<PublicParkingResponseDto> nearbyParkingLots;
        
        if (place.getLatitude() != null && place.getLongitude() != null &&
            place.getRegion() != null && place.getRegion().getKorAreaCode() != null) {
            
            String regionCode = place.getRegion().getKorAreaCode();
            if ("32".equals(regionCode)) { // 강릉시만 주차장 정보 제공
                nearbyParkingLots = publicParkingService.findNearestParking(place.getLatitude(), place.getLongitude())
                        .map(List::of)
                        .orElse(List.of());
            } else {
                nearbyParkingLots = List.of(); // 강릉시가 아니면 빈 리스트
            }
        } else {
            nearbyParkingLots = List.of();
        }
        
        return PlaceDetailWithParkingDto.from(place, nearbyParkingLots);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Place findOrCreatePlace(String contentId, String regionName, String themeName, String placeName, BigDecimal cnctrLevel) {
        return placeRepository.findByContentId(contentId)
                .orElseGet(() -> createPlace(contentId, regionName, themeName, placeName, cnctrLevel));
    }

    private Place createPlace(String contentId, String regionName, String themeName, String placeName, BigDecimal cnctrLevel) {
        // Region 찾기 - 없으면 기본값 사용
        var region = regionRepository.findByName(regionName)
                .orElse(regionRepository.findByName("강릉시")
                        .orElse(regionRepository.findAll().stream().findFirst()
                                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND))));

        // Theme 찾기 - 없으면 기본값 사용
        var theme = themeRepository.findByName(themeName)
                .orElse(themeRepository.findByName("기타")
                        .orElse(themeRepository.findAll().stream().findFirst()
                                .orElseThrow(() -> new CustomException(ErrorCode.THEME_NOT_FOUND))));

        var place = Place.builder()
                .contentId(contentId)
                .name(placeName)
                .cnctrLevel(cnctrLevel)
                .likeCount(0L)
                .region(region)
                .theme(theme)
                .build();

        var savedPlace = placeRepository.save(place);
        placeRepository.flush(); // ID가 확실히 설정되도록 flush
        return savedPlace;
    }
}
