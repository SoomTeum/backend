package com.comma.soomteum.domain.userPlace.repository;

import com.comma.soomteum.domain.userPlace.entity.UserPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPlaceRepository extends JpaRepository<UserPlace, Long> {
    Optional<UserPlace> findByUser_UserIdAndPlace_PlaceId(Long userId, Long placeId);
}
