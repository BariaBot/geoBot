package com.example.dating.backend.geo;

import com.example.dating.backend.profile.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {
    private final GeoService geoService;

    @GetMapping("/nearby")
    public List<Profile> nearby(@RequestParam double lat,
                                @RequestParam double lon,
                                @RequestParam(defaultValue = "5000") double radiusMeters,
                                @RequestParam(defaultValue = "50") int limit) {
        return geoService.findNearby(lat, lon, radiusMeters, limit);
    }
}
