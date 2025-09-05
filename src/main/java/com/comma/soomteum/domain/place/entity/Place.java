package com.comma.soomteum.domain.place.entity;

import com.comma.soomteum.domain.BaseEntity;
import com.comma.soomteum.domain.region.entity.Region;
import com.comma.soomteum.domain.theme.entity.Theme;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @Column(length = 100, nullable = false)
    @Builder.Default
    private BigDecimal cnctrLevel = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Long likeCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    public void increaseLikeCount() {
        if (this.likeCount == null) this.likeCount = 0L;
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount == null) this.likeCount = 0L;
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public Long getLikeCount() {
        return this.likeCount == null ? 0L : this.likeCount;
    }
}
