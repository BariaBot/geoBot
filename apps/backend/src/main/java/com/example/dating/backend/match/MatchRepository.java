package com.example.dating.backend.match;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    boolean existsByUser1IdAndUser2Id(Long user1Id, Long user2Id);
    List<Match> findByUser1IdOrUser2IdOrderByCreatedAtDesc(Long user1Id, Long user2Id);
}
