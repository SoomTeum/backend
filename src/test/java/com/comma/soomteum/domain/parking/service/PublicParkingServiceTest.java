package com.comma.soomteum.domain.parking.service;

import com.comma.soomteum.domain.parking.dto.PublicParkingResponseDto;
import com.comma.soomteum.domain.parking.entity.PublicParking;
import com.comma.soomteum.domain.parking.repository.PublicParkingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PublicParkingServiceTest {

    @Mock
    private PublicParkingRepository publicParkingRepository;

    @Mock
    private WebClient parkingWebClient;

    @InjectMocks
    private PublicParkingService publicParkingService;

    private PublicParking testParking1;
    private PublicParking testParking2;

    @BeforeEach
    void setUp() {
        testParking1 = new PublicParking(
                "GANG001", 
                "성내동광장주차장", 
                100, 
                50,
                new BigDecimal("37.7519"), 
                new BigDecimal("128.8761"), 
                "32230"
        );
        
        testParking2 = new PublicParking(
                "GANG002", 
                "강문제2공영주차장", 
                80, 
                30,
                new BigDecimal("37.7881"), 
                new BigDecimal("128.9342"), 
                "32230"
        );
    }

    @Test
    @DisplayName("지역 코드로 주차장 목록 조회")
    void findByRegion() {
        // given
        String regionCode = "32230";
        List<PublicParking> mockParkingList = Arrays.asList(testParking1, testParking2);
        given(publicParkingRepository.findByRegionCode(regionCode)).willReturn(mockParkingList);

        // when
        List<PublicParkingResponseDto> result = publicParkingService.findByRegion(regionCode);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPrkName()).isEqualTo("성내동광장주차장");
        assertThat(result.get(1).getPrkName()).isEqualTo("강문제2공영주차장");
    }

    @Test
    @DisplayName("주변 주차장 조회")
    void findNearbyParking() {
        // given
        BigDecimal latitude = new BigDecimal("37.7500");
        BigDecimal longitude = new BigDecimal("128.8800");
        String regionCode = "32230";
        int limit = 5;
        
        List<PublicParking> mockParkingList = Arrays.asList(testParking1, testParking2);
        given(publicParkingRepository.findNearbyParkingLots(eq(latitude), eq(longitude), eq(regionCode), eq(limit)))
                .willReturn(mockParkingList);

        // when
        List<PublicParkingResponseDto> result = publicParkingService.findNearbyParking(latitude, longitude, regionCode, limit);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDistance()).isNotNull();
        assertThat(result.get(1).getDistance()).isNotNull();
    }
}