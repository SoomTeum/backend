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

    @Column(nullable = false)
    private Integer areaCode;

    @Column(nullable = false)
    private Integer sigunguCode;

    @Column(length = 100, nullable = false)
    private String name;
}
