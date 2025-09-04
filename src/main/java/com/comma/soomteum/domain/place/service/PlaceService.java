package com.comma.soomteum.domain.place.service;

import com.comma.soomteum.domain.parking.dto.PublicParkingResponseDto;
import com.comma.soomteum.domain.parking.service.PublicParkingService;
import com.comma.soomteum.domain.place.dto.response.PlaceDetailWithParkingDto;
import com.comma.soomteum.domain.place.entity.Place;
import com.comma.soomteum.domain.place.repository.PlaceRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PublicParkingService publicParkingService;

    public Place findPlaceById(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));
    }

    public Place findPlaceByContentId(String contentId) {
        return placeRepository.findByContentId(contentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));
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
}
