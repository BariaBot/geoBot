package com.example.dating.backend.geo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GeoLocationRepository extends JpaRepository<GeoLocation, Long> {
    @Query(value = "SELECT user_id FROM geo WHERE ST_DWithin(point, ST_MakePoint(:lon, :lat)::geography, :radius) ORDER BY ST_Distance(point, ST_MakePoint(:lon, :lat)::geography) LIMIT :limit", nativeQuery = true)
    List<Long> findNearby(@Param("lat") double lat, @Param("lon") double lon, @Param("radius") double radiusMeters, @Param("limit") int limit);
}
