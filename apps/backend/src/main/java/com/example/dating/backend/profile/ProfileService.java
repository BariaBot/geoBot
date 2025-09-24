package com.example.dating.backend.profile;

import com.example.dating.backend.geo.GeoLocation;
import com.example.dating.backend.geo.GeoLocationRepository;
import com.example.dating.backend.user.User;
import com.example.dating.backend.user.UserService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserService userService;
    private final ProfileRepository profileRepository;
    private final ProfileInterestRepository interestRepository;
    private final GeoLocationRepository geoLocationRepository;
    private final GeometryFactory geometryFactory;

    @Transactional(readOnly = true)
    public ProfileResponse fetchProfile(Long telegramId) {
        User user = userService.ensureUserExists(telegramId, null);
        Profile profile = profileRepository.findById(user.getId())
                .orElseGet(() -> createDefaultProfile(user));
        GeoLocation geo = geoLocationRepository.findById(user.getId()).orElse(null);

        List<String> interests = interestRepository.findByUserId(user.getId()).stream()
                .map(ProfileInterestEntity::getValue)
                .toList();

        return mapToResponse(user, profile, geo, interests);
    }

    @Transactional
    public ProfileResponse upsertProfile(Long telegramId, UpdateProfileRequest request) {
        User user = userService.ensureUserExists(telegramId, request.displayName());
        Profile profile = profileRepository.findById(user.getId())
                .orElseGet(() -> createDefaultProfile(user));

        applyProfilePayload(profile, request);
        profileRepository.save(profile);

        GeoLocation geo = upsertLocation(user, request);
        List<String> interests = upsertInterests(user.getId(), request.interests());

        log.info("profile_updated telegramId={} interests={} city={}", telegramId, interests, request.city());

        return mapToResponse(user, profile, geo, interests);
    }

    private Profile createDefaultProfile(User user) {
        Profile profile = Profile.builder()
                .user(user)
                .userId(user.getId())
                .bio("")
                .isVip(false)
                .build();
        user.setProfile(profile);
        return profile;
    }

    private void applyProfilePayload(Profile profile, UpdateProfileRequest request) {
        profile.setBio(request.bio());
        profile.setCity(request.city());
        profile.setBirthday(request.birthday());
        profile.setGender(parseGender(request.gender()));
    }

    private Profile.Gender parseGender(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return Profile.Gender.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private GeoLocation upsertLocation(User user, UpdateProfileRequest request) {
        if (request.latitude() == null || request.longitude() == null) {
            return geoLocationRepository.findById(user.getId()).orElse(null);
        }

        GeoLocation geo = geoLocationRepository.findById(user.getId())
                .orElseGet(() -> GeoLocation.builder()
                        .user(user)
                        .userId(user.getId())
                        .build());

        geo.setPoint(geometryFactory.createPoint(new Coordinate(request.longitude(), request.latitude())));
        geo.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC).toInstant());

        return geoLocationRepository.save(geo);
    }

    private List<String> upsertInterests(Long userId, List<String> interests) {
        interestRepository.deleteByUserId(userId);

        if (interests == null || interests.isEmpty()) {
            return List.of();
        }

        return interests.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    ProfileInterestEntity entity = new ProfileInterestEntity();
                    entity.setUserId(userId);
                    entity.setValue(value);
                    return interestRepository.save(entity);
                })
                .map(ProfileInterestEntity::getValue)
                .toList();
    }

    private ProfileResponse mapToResponse(User user, Profile profile, GeoLocation geo, List<String> interests) {
        ProfileResponse.LocationPayload location = null;
        if (geo != null && geo.getPoint() != null) {
            location = ProfileResponse.LocationPayload.builder()
                    .latitude(geo.getPoint().getY())
                    .longitude(geo.getPoint().getX())
                    .updatedAt(OffsetDateTime.ofInstant(geo.getUpdatedAt(), ZoneOffset.UTC))
                    .build();
        }

        String displayName = user.getUsername() != null && !user.getUsername().isBlank()
                ? user.getUsername()
                : String.valueOf(user.getTelegramId());

        return ProfileResponse.builder()
                .telegramId(user.getTelegramId())
                .displayName(displayName)
                .bio(profile.getBio())
                .gender(profile.getGender() != null ? profile.getGender().name() : null)
                .birthday(profile.getBirthday())
                .city(profile.getCity())
                .vip(profile.isVip())
                .vipUntil(profile.getVipUntil())
                .location(location)
                .interests(interests)
                .build();
    }
}
