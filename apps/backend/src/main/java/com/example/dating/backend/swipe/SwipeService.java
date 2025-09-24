package com.example.dating.backend.swipe;

import com.example.dating.backend.geo.GeoLocation;
import com.example.dating.backend.geo.GeoLocationRepository;
import com.example.dating.backend.match.Like;
import com.example.dating.backend.match.LikeRepository;
import com.example.dating.backend.match.Match;
import com.example.dating.backend.match.MatchRepository;
import com.example.dating.backend.profile.Profile;
import com.example.dating.backend.profile.ProfileInterestEntity;
import com.example.dating.backend.profile.ProfileInterestRepository;
import com.example.dating.backend.profile.ProfileRepository;
import com.example.dating.backend.user.User;
import com.example.dating.backend.user.UserRepository;
import com.example.dating.backend.user.UserService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SwipeService {

    private static final int DEFAULT_FEED_SIZE = 20;

    private final UserService userService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ProfileInterestRepository interestRepository;
    private final GeoLocationRepository geoRepository;
    private final LikeRepository likeRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public SwipeFeedResponse loadFeed(Long telegramId, Integer limitOverride) {
        User user = userService.ensureUserExists(telegramId, null);
        GeoLocation viewerGeo = geoRepository.findById(user.getId()).orElse(null);

        Set<Long> likedUserIds = likeRepository.findByFromUserId(user.getId()).stream()
                .map(Like::getToUserId)
                .collect(Collectors.toSet());

        int feedSize = limitOverride != null && limitOverride > 0 ? limitOverride : DEFAULT_FEED_SIZE;
        int fetchSize = Math.min(feedSize * 2, 100);

        List<Profile> candidates = profileRepository
                .findByUserIdNot(user.getId(), PageRequest.of(0, fetchSize))
                .getContent().stream()
                .filter(candidate -> !likedUserIds.contains(candidate.getUserId()))
                .limit(feedSize)
                .toList();

        if (candidates.isEmpty()) {
            return SwipeFeedResponse.builder()
                    .timestamp(OffsetDateTime.now())
                    .items(List.of())
                    .build();
        }

        Map<Long, List<String>> interests = loadInterests(candidates);
        Map<Long, GeoLocation> geoByUserId = geoRepository.findAllById(
                        candidates.stream().map(Profile::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(GeoLocation::getUserId, Function.identity()));

        List<SwipeFeedItem> items = new ArrayList<>();
        for (Profile candidate : candidates) {
            if (candidate.getUserId().equals(user.getId())) {
                continue;
            }

            GeoLocation geo = geoByUserId.get(candidate.getUserId());
            items.add(SwipeFeedItem.from(
                    candidate,
                    interests.getOrDefault(candidate.getUserId(), List.of()),
                    viewerGeo,
                    geo));
        }

        items.sort(Comparator.comparing(item -> Optional.ofNullable(item.distanceMeters()).orElse(Double.MAX_VALUE)));

        return SwipeFeedResponse.builder()
                .timestamp(OffsetDateTime.now())
                .items(items)
                .build();
    }

    @Transactional
    public SwipeDecisionResponse like(Long telegramId, Long targetTelegramId) {
        User actor = userService.ensureUserExists(telegramId, null);
        User target = userRepository.findByTelegramId(targetTelegramId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        boolean alreadyLiked = likeRepository.existsByFromUserIdAndToUserId(actor.getId(), target.getId());
        if (!alreadyLiked) {
            likeRepository.save(Like.builder()
                    .fromUserId(actor.getId())
                    .toUserId(target.getId())
                    .createdAt(OffsetDateTime.now())
                    .build());
        }

        boolean reciprocal = likeRepository.existsByFromUserIdAndToUserId(target.getId(), actor.getId());
        if (reciprocal) {
            long u1 = Math.min(actor.getId(), target.getId());
            long u2 = Math.max(actor.getId(), target.getId());
            if (!matchRepository.existsByUser1IdAndUser2Id(u1, u2)) {
                matchRepository.save(Match.builder()
                        .user1Id(u1)
                        .user2Id(u2)
                        .createdAt(OffsetDateTime.now())
                        .build());
            }
            return SwipeDecisionResponse.builder()
                    .match(true)
                    .targetTelegramId(targetTelegramId)
                    .build();
        }

        return SwipeDecisionResponse.builder()
                .match(false)
                .targetTelegramId(targetTelegramId)
                .build();
    }

    private Map<Long, List<String>> loadInterests(Collection<Profile> candidates) {
        Set<Long> userIds = candidates.stream().map(Profile::getUserId).collect(Collectors.toSet());
        List<ProfileInterestEntity> interests = interestRepository.findByUserIdIn(userIds);
        return interests.stream().collect(Collectors.groupingBy(ProfileInterestEntity::getUserId,
                Collectors.mapping(ProfileInterestEntity::getValue, Collectors.toList())));
    }
}
