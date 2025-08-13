package com.example.dating.backend.match;

import com.example.dating.backend.user.User;
import com.example.dating.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final LikeRepository likeRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean like(Long fromTelegramId, Long targetUserId) {
        User from = userRepository.findByTelegramId(fromTelegramId)
                .orElseThrow();
        Long fromId = from.getId();
        if (likeRepository.existsByFromUserIdAndToUserId(fromId, targetUserId)) {
            return false;
        }
        likeRepository.save(Like.builder()
                .fromUserId(fromId)
                .toUserId(targetUserId)
                .createdAt(OffsetDateTime.now())
                .build());
        if (likeRepository.existsByFromUserIdAndToUserId(targetUserId, fromId)) {
            long u1 = Math.min(fromId, targetUserId);
            long u2 = Math.max(fromId, targetUserId);
            if (!matchRepository.existsByUser1IdAndUser2Id(u1, u2)) {
                matchRepository.save(Match.builder()
                        .user1Id(u1)
                        .user2Id(u2)
                        .createdAt(OffsetDateTime.now())
                        .build());
            }
            return true;
        }
        return false;
    }

    public List<Match> getMatches(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId).orElseThrow();
        Long id = user.getId();
        return matchRepository.findByUser1IdOrUser2IdOrderByCreatedAtDesc(id, id);
    }
}
