package com.comma.soomteum.domain.place.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.comma.soomteum.domain.place.entity.Place;
import com.comma.soomteum.domain.place.repository.PlaceRepository;
import com.comma.soomteum.global.response.CustomException;
import com.comma.soomteum.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    public Place findPlaceById(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));
    }
}
