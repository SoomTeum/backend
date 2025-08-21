package com.comma.soomteum.domain.region.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "region_cnctr",
        indexes = {
                @Index(name = "ix_region_cnctr_region", columnList = "region_id"),
                @Index(name = "ix_region_cnctr_code",   columnList = "cnctr_sigungu_code")
        }
)
public class RegionCnctr {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="region_id", nullable=false)
  private Region region;

  @Column(name="cnctr_sigungu_code", nullable=false, length=10)
  private String cnctrSigunguCode;

  @Column(nullable=false) private int priority; // 1,2,3...
}
