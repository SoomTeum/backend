package com.comma.soomteum.domain.parking.entity;

import com.comma.soomteum.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "public_parking")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicParking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prk_id", nullable = false, unique = true)
    private String prkId;

    @Column(name = "prk_name", nullable = false)
    private String prkName;

    @Column(name = "total_lots")
    private Integer totalLots;

    @Column(name = "avail_lots")
    private Integer availLots;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "region_code")
    private String regionCode;

    public PublicParking(String prkId, String prkName, Integer totalLots, Integer availLots,
                        BigDecimal latitude, BigDecimal longitude, String regionCode) {
        this.prkId = prkId;
        this.prkName = prkName;
        this.totalLots = totalLots;
        this.availLots = availLots;
        this.latitude = latitude;
        this.longitude = longitude;
        this.regionCode = regionCode;
    }

    public void updateAvailability(Integer totalLots, Integer availLots) {
        this.totalLots = totalLots;
        this.availLots = availLots;
    }
}