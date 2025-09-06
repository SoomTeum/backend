package com.comma.soomteum.domain.parking.repository;

import com.comma.soomteum.domain.parking.entity.PublicParking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicParkingRepository extends JpaRepository<PublicParking, Long> {

    Optional<PublicParking> findByPrkId(String prkId);

    List<PublicParking> findByRegionCode(String regionCode);

    @Query(value = """
        SELECT *, 
               (6371 * acos(cos(radians(:latitude)) * cos(radians(latitude)) 
                        * cos(radians(longitude) - radians(:longitude)) 
                        + sin(radians(:latitude)) * sin(radians(latitude)))) AS distance
        FROM public_parking
        WHERE region_code = :regionCode
        ORDER BY distance
        LIMIT :limit
    """, nativeQuery = true)
    List<PublicParking> findNearbyParkingLots(@Param("latitude") BigDecimal latitude,
                                            @Param("longitude") BigDecimal longitude,
                                            @Param("regionCode") String regionCode,
                                            @Param("limit") int limit);
}