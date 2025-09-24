package com.example.dating.backend.swipe;

import com.example.dating.backend.geo.GeoLocation;
import com.example.dating.backend.profile.Profile;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.Builder;

@Builder
public record SwipeFeedItem(
        Long telegramId,
        String displayName,
        String bio,
        String city,
        String gender,
        Double distanceMeters,
        OffsetDateTime lastSeen,
        List<String> interests
) {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public static SwipeFeedItem from(
            Profile candidate,
            List<String> interests,
            GeoLocation viewerGeo,
            GeoLocation candidateGeo
    ) {
        Double distance = null;
        OffsetDateTime lastSeen = null;

        if (viewerGeo != null && candidateGeo != null
                && viewerGeo.getPoint() != null && candidateGeo.getPoint() != null) {
            distance = computeDistanceMeters(viewerGeo, candidateGeo);
            lastSeen = OffsetDateTime.ofInstant(candidateGeo.getUpdatedAt(), ZoneOffset.UTC);
        }

        String displayName = candidate.getUser().getUsername();
        if (displayName == null || displayName.isBlank()) {
            displayName = String.valueOf(candidate.getUser().getTelegramId());
        }

        return SwipeFeedItem.builder()
                .telegramId(candidate.getUser().getTelegramId())
                .displayName(displayName)
                .bio(candidate.getBio())
                .city(candidate.getCity())
                .gender(candidate.getGender() != null ? candidate.getGender().name() : null)
                .distanceMeters(distance)
                .lastSeen(lastSeen)
                .interests(interests)
                .build();
    }

    private static double computeDistanceMeters(GeoLocation a, GeoLocation b) {
        double lat1 = Math.toRadians(a.getPoint().getY());
        double lon1 = Math.toRadians(a.getPoint().getX());
        double lat2 = Math.toRadians(b.getPoint().getY());
        double lon2 = Math.toRadians(b.getPoint().getX());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);

        double aTerm = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double c = 2 * Math.atan2(Math.sqrt(aTerm), Math.sqrt(1 - aTerm));
        return EARTH_RADIUS_METERS * c;
    }
}
