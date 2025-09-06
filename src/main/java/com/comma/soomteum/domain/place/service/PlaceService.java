package com.comma.soomteum.domain.place.service;

import com.comma.soomteum.domain.place.entity.Place;
import com.comma.soomteum.domain.place.repository.PlaceRepository;
import com.comma.soomteum.domain.region.repository.RegionRepository;
import com.comma.soomteum.domain.theme.repository.ThemeRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;

import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final RegionRepository regionRepository;
    private final ThemeRepository themeRepository;

    public Place findPlaceById(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));
    }

    public Optional<Place> findByContentId(String contentId) {
        return placeRepository.findByContentId(contentId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Place findOrCreatePlace(String contentId, Long regionId, Long themeId, BigDecimal cnctrLevel) {
        return placeRepository.findByContentId(contentId)
                .orElseGet(() -> createPlace(contentId, regionId, themeId, cnctrLevel));
    }

    private Place createPlace(String contentId, Long regionId, Long themeId, BigDecimal cnctrLevel) {
        var region = regionRepository.findById(regionId)
                .orElseThrow(() -> new CustomException(ErrorCode.REGION_NOT_FOUND));
        
        var theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new CustomException(ErrorCode.THEME_NOT_FOUND));

        var place = Place.builder()
                .contentId(contentId)
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
