package com.comma.soomteum.domain.region.service;

import com.comma.soomteum.config.CacheConfig;
import com.comma.soomteum.domain.region.dto.RegionGroupResponseDto;
import com.comma.soomteum.domain.region.entity.Region;
import com.comma.soomteum.domain.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegionService {

    private final RegionRepository regionRepository;

    public List<RegionGroupResponseDto> getAllRegions() {
        List<Region> regions = regionRepository.findAll();
        
        Map<String, List<Region>> groupedByAreaCode = regions.stream()
                .collect(Collectors.groupingBy(Region::getKorAreaCode));

        return groupedByAreaCode.entrySet().stream()
                .map(entry -> {
                    String areaCode = entry.getKey();
                    List<Region> areaRegions = entry.getValue();

                    List<RegionGroupResponseDto.SigunguInfo> sigunguList = areaRegions.stream()
                            .map(region -> RegionGroupResponseDto.SigunguInfo.builder()
                                    .sigunguCode(region.getKorSigunguCode())
                                    .sigunguName(region.getName())
                                    .build())
                            .collect(Collectors.toList());

                    return RegionGroupResponseDto.builder()
                            .areaCode(areaCode)
                            .areaName(getAreaName(areaCode))
                            .sigunguList(sigunguList)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getAreaName(String areaCode) {
        if (areaCode == null) {
            return "정보없음";
        }

        switch (areaCode) {
            case "1":
                return "서울";
            case "2":
                return "인천";
            case "3":
                return "대전";
            case "4":
                return "대구";
            case "5":
                return "광주";
            case "6":
                return "부산";
            case "7":
                return "울산";
            case "8":
                return "세종";
            case "31":
                return "경기도";
            case "32":
                return "강원";
            default:
                return "기타";
        }
    }

    /**
     * areaCode, sigunguCode 기준 지역 조회 (캐시 적용)
     *
     * 캐시 키: areaCode:sigunguCode
     * TTL: 24시간
     */
    @Cacheable(
            cacheNames = CacheConfig.REGION_CACHE,
            key = "#areaCode + ':' + #sigunguCode"
    )
    public Optional<Region> findByAreaCodeAndSigunguCode(String areaCode, String sigunguCode) {
        log.debug("[RegionService] 지역 조회: areaCode={}, sigunguCode={}", areaCode, sigunguCode);
        return regionRepository.findByKorAreaCodeAndKorSigunguCode(areaCode, sigunguCode);
    }

    /**
     * areaCode, sigunguCode로 지역 이름 조회 (캐시 적용)
     *
     * @return 지역 이름 또는 null
     */
    @Cacheable(
            cacheNames = CacheConfig.REGION_CACHE,
            key = "'name:' + #areaCode + ':' + #sigunguCode"
    )
    public String getRegionName(String areaCode, String sigunguCode) {
        log.debug("[RegionService] 지역 이름 조회: areaCode={}, sigunguCode={}", areaCode, sigunguCode);
        return regionRepository.findByKorAreaCodeAndKorSigunguCode(areaCode, sigunguCode)
                .map(Region::getName)
                .orElse(null);
    }
}