package com.comma.soomteum.domain.region.entity;

import com.comma.soomteum.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Region extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long regionId;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(name = "area_code", nullable = false)
    private String areaCode;   // 기본 지역 코드

    @Column(name = "kor_area_code", nullable = false)
    private String korAreaCode;   // 국문 관광 서비스 지역코드

    @Column(name = "kor_sigungu_code", nullable = false)
    private String korSigunguCode;   // 국문 관광 서비스 시군구 코드

    @Column(name = "cnctr_area_code", nullable = false)
    private String cnctrAreaCode;   // 집중률 지역 코드

    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("priority ASC, cnctrSigunguCode ASC")
    private List<RegionCnctr> cnctrSigungus;
}
