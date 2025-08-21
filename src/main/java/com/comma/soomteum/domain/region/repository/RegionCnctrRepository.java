package com.comma.soomteum.domain.region.repository;

import com.comma.soomteum.domain.region.entity.Region;
import com.comma.soomteum.domain.region.entity.RegionCnctr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegionCnctrRepository extends JpaRepository<RegionCnctr, Long> {
  List<RegionCnctr> findAllByRegionOrderByPriorityAsc(Region region);
}
