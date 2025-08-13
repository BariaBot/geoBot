package com.example.dating.backend.match;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByFromUserIdAndToUserId(Long fromUserId, Long toUserId);
}
