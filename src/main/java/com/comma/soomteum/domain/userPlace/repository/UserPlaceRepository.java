package com.comma.soomteum.domain.userPlace.repository;

import com.comma.soomteum.domain.userPlace.entity.UserPlace;
import com.comma.soomteum.domain.userPlace.enums.UserActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPlaceRepository extends JpaRepository<UserPlace, Long> {

    @Deprecated
    Optional<UserPlace> findByUser_UserIdAndPlace_PlaceId(Long userId, Long placeId);

    Optional<UserPlace> findByUser_UserIdAndPlace_PlaceIdAndType(Long userId, Long placeId, UserActionType type);

    boolean existsByUser_UserIdAndPlace_PlaceIdAndType(Long userId, Long placeId, UserActionType type);

    long countByPlace_PlaceIdAndType(Long placeId, UserActionType type);

    @EntityGraph(attributePaths = "place")
    Page<UserPlace> findByUser_UserIdAndType(Long userId, UserActionType type, Pageable pageable);
}
