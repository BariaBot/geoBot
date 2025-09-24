package com.example.dating.backend.match;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    List<Like> findByFromUserId(Long fromUserId);

    List<Like> findByToUserId(Long toUserId);
}
