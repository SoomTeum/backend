package com.comma.soomteum.domain.place.repository;

import com.comma.soomteum.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
}
