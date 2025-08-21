package com.comma.soomteum.domain.region.repository;

import com.comma.soomteum.domain.region.entity.Region;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {
    @EntityGraph(attributePaths = "cnctrSigungus")
    Optional<Region> findByKorAreaCodeAndKorSigunguCode(String korAreaCode, String korSigunguCode);
}
