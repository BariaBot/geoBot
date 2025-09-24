package com.example.dating.backend.geo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GeoLocationRepository extends JpaRepository<GeoLocation, Long> {

    @Query(value = """
            SELECT user_id
            FROM geo
            WHERE ST_DWithin(
                point,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                :radiusMeters
            )
            ORDER BY point <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findNearby(
            @Param("lat") double latitude,
            @Param("lon") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit
    );
}
