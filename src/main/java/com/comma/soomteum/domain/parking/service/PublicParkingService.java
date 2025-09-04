package com.comma.soomteum.domain.parking.service;

import com.comma.soomteum.domain.parking.dto.ParkingApiResponse;
import com.comma.soomteum.domain.parking.dto.PublicParkingResponseDto;
import com.comma.soomteum.domain.parking.entity.PublicParking;
import com.comma.soomteum.domain.parking.repository.PublicParkingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PublicParkingService {

    private final PublicParkingRepository publicParkingRepository;
    private final WebClient parkingWebClient;

    @Value("${parking.api.key:}")
    private String apiKey;

    @Value("${parking.api.url:https://www.parking.go.kr/api/getParkRltm}")
    private String apiUrl;

    public List<PublicParkingResponseDto> findNearbyParking(BigDecimal latitude, BigDecimal longitude, String regionCode, int limit) {
        List<PublicParking> nearbyParkingLots = publicParkingRepository.findNearbyParkingLots(latitude, longitude, regionCode, limit);
        
        updateParkingAvailability();
        
        return nearbyParkingLots.stream()
                .map(parking -> {
                    BigDecimal distance = calculateDistance(latitude, longitude, parking.getLatitude(), parking.getLongitude());
                    return PublicParkingResponseDto.from(parking, distance);
                })
                .toList();
    }

    @Transactional
    public void updateParkingAvailability() {
        try {
            ParkingApiResponse response = parkingWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getParkingInfo")
                            .queryParam("serviceKey", apiKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 100)
                            .queryParam("_type", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(ParkingApiResponse.class)
                    .block();

            if (response != null && response.getResponse() != null && 
                response.getResponse().getBody() != null && 
                response.getResponse().getBody().getItems() != null) {
                
                response.getResponse().getBody().getItems().forEach(item -> {
                    Optional<PublicParking> existingParking = publicParkingRepository.findByPrkId(item.getPrkId());
                    if (existingParking.isPresent()) {
                        existingParking.get().updateAvailability(item.getTotalLots(), item.getAvailLots());
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to update parking availability", e);
        }
    }

    private BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return BigDecimal.ZERO;
        }

        double earthRadius = 6371;
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double deltaLatRad = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLonRad = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadius * c;
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

    public List<PublicParkingResponseDto> findByRegion(String regionCode) {
        List<PublicParking> parkingLots = publicParkingRepository.findByRegionCode(regionCode);
        return parkingLots.stream()
                .map(PublicParkingResponseDto::from)
                .toList();
    }

    public Optional<PublicParkingResponseDto> findNearestParking(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }

        // 실시간 정보 업데이트 시도 (실패해도 계속 진행)
        try {
            updateParkingAvailability();
        } catch (Exception e) {
            log.warn("Failed to update parking availability, using cached data: {}", e.getMessage());
        }

        List<PublicParking> nearbyParkingLots = publicParkingRepository.findNearbyParkingLots(
            latitude, longitude, "32230", 1);

        return nearbyParkingLots.stream()
                .findFirst()
                .map(parking -> {
                    BigDecimal distance = calculateDistance(latitude, longitude, parking.getLatitude(), parking.getLongitude());
                    return PublicParkingResponseDto.from(parking, distance);
                });
    }
}