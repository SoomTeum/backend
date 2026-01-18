package com.comma.soomteum.domain.theme.service;

import com.comma.soomteum.config.CacheConfig;
import com.comma.soomteum.domain.theme.dto.ThemeGroupResponseDto;
import com.comma.soomteum.domain.theme.entity.Theme;
import com.comma.soomteum.domain.theme.repository.ThemeRepository;
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
public class ThemeService {

    private final ThemeRepository themeRepository;

    public List<ThemeGroupResponseDto> getAllThemes() {
        List<Theme> themes = themeRepository.findAll();
        
        Map<String, List<Theme>> groupedByCat1 = themes.stream()
                .collect(Collectors.groupingBy(Theme::getCat1));

        return groupedByCat1.entrySet().stream()
                .map(entry -> {
                    String cat1 = entry.getKey();
                    List<Theme> cat1Themes = entry.getValue();

                    List<ThemeGroupResponseDto.Cat2Info> cat2List = cat1Themes.stream()
                            .map(theme -> ThemeGroupResponseDto.Cat2Info.builder()
                                    .cat2(theme.getCat2())
                                    .cat2Name(theme.getName())
                                    .build())
                            .collect(Collectors.toList());

                    return ThemeGroupResponseDto.builder()
                            .cat1(cat1)
                            .cat1Name(getCat1Name(cat1))
                            .cat2List(cat2List)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getCat1Name(String cat1) {
        if (cat1 == null) {
            return "정보없음";
        }

        switch (cat1) {
            case "A01":
                return "자연";
            case "A02":
                return "인문";
            case "A03":
                return "레포츠";
            case "A04":
                return "쇼핑";
            default:
                return "기타";
        }
    }

    /**
     * cat1, cat2 기준 테마 조회 (캐시 적용)
     *
     * 캐시 키: cat1:cat2
     * TTL: 24시간
     */
    @Cacheable(
            cacheNames = CacheConfig.THEME_CACHE,
            key = "#cat1 + ':' + #cat2"
    )
    public Optional<Theme> findByCat1AndCat2(String cat1, String cat2) {
        log.debug("[ThemeService] 테마 조회: cat1={}, cat2={}", cat1, cat2);
        return themeRepository.findByCat1AndCat2(cat1, cat2);
    }

    /**
     * cat1, cat2로 테마 이름 조회 (캐시 적용)
     *
     * @return 테마 이름 또는 null
     */
    @Cacheable(
            cacheNames = CacheConfig.THEME_CACHE,
            key = "'name:' + #cat1 + ':' + #cat2"
    )
    public String getThemeName(String cat1, String cat2) {
        log.debug("[ThemeService] 테마 이름 조회: cat1={}, cat2={}", cat1, cat2);
        return themeRepository.findByCat1AndCat2(cat1, cat2)
                .map(Theme::getName)
                .orElse(null);
    }
}