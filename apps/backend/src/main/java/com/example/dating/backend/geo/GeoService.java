package com.example.dating.backend.geo;

import com.example.dating.backend.profile.Profile;
import com.example.dating.backend.profile.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeoService {
    private final GeoLocationRepository geoRepository;
    private final ProfileRepository profileRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public void updateLocation(Long userId, double lat, double lon) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        point.setSRID(4326);
        GeoLocation geo = geoRepository.findById(userId).orElseGet(() -> GeoLocation.builder().userId(userId).build());
        geo.setPoint(point);
        geo.setUpdatedAt(Instant.now());
        geoRepository.save(geo);
    }

    public List<Profile> findNearby(double lat, double lon, double radiusMeters, int limit) {
        List<Long> ids = geoRepository.findNearby(lat, lon, radiusMeters, limit);
        return profileRepository.findAllById(ids);
    }
}
