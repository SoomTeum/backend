package com.comma.soomteum.domain.place.dto.response;

import com.comma.soomteum.domain.parking.dto.PublicParkingResponseDto;
import com.comma.soomteum.domain.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class PlaceDetailWithParkingDto {
    private Long placeId;
    private String contentId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long likeCount;
    private String regionName;
    private String themeName;
    private List<PublicParkingResponseDto> nearbyParkingLots;

    public static PlaceDetailWithParkingDto from(Place place, List<PublicParkingResponseDto> nearbyParkingLots) {
        return PlaceDetailWithParkingDto.builder()
                .placeId(place.getPlaceId())
                .contentId(place.getContentId())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .likeCount(place.getLikeCount())
                .regionName(place.getRegion().getName())
                .themeName(place.getTheme().getName())
                .nearbyParkingLots(nearbyParkingLots)
                .build();
    }
}