package com.comma.soomteum.config;

import com.comma.soomteum.domain.parking.entity.PublicParking;
import com.comma.soomteum.domain.parking.repository.PublicParkingRepository;
import com.comma.soomteum.domain.place.entity.Place;
import com.comma.soomteum.domain.place.repository.PlaceRepository;
import com.comma.soomteum.domain.region.entity.Region;
import com.comma.soomteum.domain.region.repository.RegionRepository;
import com.comma.soomteum.domain.theme.entity.Theme;
import com.comma.soomteum.domain.theme.repository.ThemeRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod") // 운영환경에서는 실행하지 않음
public class DataInitializer {

    private final RegionRepository regionRepository;
    private final ThemeRepository themeRepository;
    private final PlaceRepository placeRepository;
    private final PublicParkingRepository publicParkingRepository;

    @PostConstruct
    @Transactional
    public void initializeTestData() {
        log.info("Initializing test data...");
        
        // Region 데이터 생성
        Region gangneung = regionRepository.findByName("강릉시").orElse(null);
        if (gangneung == null) {
            gangneung = new Region(null, "강릉시", "32", "32230", "32", "32230", "32230", "32230", null);
            gangneung = regionRepository.save(gangneung);
            log.info("Created region: {}", gangneung.getName());
        }

        // Theme 데이터 생성
        Theme theme = themeRepository.findById(1L).orElse(null);
        if (theme == null) {
            theme = Theme.builder()
                    .cat1("A01")
                    .cat2("A0101")
                    .name("자연관광지")
                    .build();
            themeRepository.save(theme);
            log.info("Created theme: {}", theme.getName());
        }
        
        // Place 테스트 데이터 생성
        if (placeRepository.findByContentId("264670").isEmpty()) {
            Place place = Place.builder()
                    .contentId("264670")
                    .cnctrLevel(new BigDecimal("0"))
                    .latitude(new BigDecimal("37.8056"))
                    .longitude(new BigDecimal("128.9084"))
                    .likeCount(0L)
                    .region(gangneung)
                    .theme(theme)
                    .build();
            placeRepository.save(place);
            log.info("Created place: contentId={}", place.getContentId());
        }

        // 공영주차장 테스트 데이터 생성
        createParkingDataIfNotExists();
        
        log.info("Test data initialization completed.");
    }

    private void createParkingDataIfNotExists() {
        if (publicParkingRepository.count() == 0) {
            // 강릉시 주요 주차장 데이터
            createParkingLot("GANG001", "성내동광장주차장", 100, 50, "37.7519", "128.8761");
            createParkingLot("GANG002", "강문제2공영주차장", 80, 30, "37.7881", "128.9342");
            createParkingLot("GANG003", "주문진해안주차타워", 150, 80, "37.8969", "128.8269");
            createParkingLot("GANG011", "경포아쿠아리움공영주차장", 180, 90, "37.8056", "128.9084");
            createParkingLot("GANG013", "동부시장공영주차장", 95, 48, "37.7565", "128.8801");
            
            log.info("Created {} parking lots", publicParkingRepository.count());
        }
    }

    private void createParkingLot(String prkId, String prkName, int totalLots, int availLots, 
                                 String latitude, String longitude) {
        PublicParking parking = new PublicParking(
                prkId, 
                prkName, 
                totalLots, 
                availLots,
                new BigDecimal(latitude), 
                new BigDecimal(longitude), 
                "32230"
        );
        publicParkingRepository.save(parking);
    }
}