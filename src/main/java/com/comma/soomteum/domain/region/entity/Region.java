package com.comma.soomteum.domain.region.entity;

import com.comma.soomteum.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Region extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long regionId;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(name = "kor_area_code", nullable = false)
    private String korAreaCode;   // 국문 관광 서비스 지역코드

    @Column(name = "kor_sigungu_code", nullable = false)
    private String korSigunguCode;   // 국문 관광 서비스 시군구 코드

    @Column(name = "cnctr_area_code", nullable = false)
    private int cnctrAreaCode;   // 집중률 지역 코드

    @Column(name = "cnctr_sigungu_code", nullable = false)
    private int cnctrSigunguCode;   // 집중률 시군구 코드

}
