package com.example.dating.backend.profile;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUserTelegramId(Long telegramId);

    @EntityGraph(attributePaths = "user")
    Page<Profile> findByUserIdNot(Long userId, Pageable pageable);
}
