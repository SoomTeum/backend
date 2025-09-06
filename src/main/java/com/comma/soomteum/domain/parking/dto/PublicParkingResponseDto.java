package com.comma.soomteum.domain.parking.dto;

import com.comma.soomteum.domain.parking.entity.PublicParking;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PublicParkingResponseDto {
    private String prkId;
    private String prkName;
    private Integer totalLots;
    private Integer availLots;
    private BigDecimal distance;

    public static PublicParkingResponseDto from(PublicParking parking, BigDecimal distance) {
        return new PublicParkingResponseDto(
                parking.getPrkId(),
                parking.getPrkName(),
                parking.getTotalLots(),
                parking.getAvailLots(),
                distance
        );
    }

    public static PublicParkingResponseDto from(PublicParking parking) {
        return new PublicParkingResponseDto(
                parking.getPrkId(),
                parking.getPrkName(),
                parking.getTotalLots(),
                parking.getAvailLots(),
                null
        );
    }
}