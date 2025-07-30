package com.comma.soomteum.domain.place.entity;

import com.comma.soomteum.domain.BaseEntity;
import com.comma.soomteum.domain.region.entity.Region;
import com.comma.soomteum.domain.theme.entity.Theme;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Place extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placeId;

    @Column(length = 255, nullable = false)
    private String contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;
}
