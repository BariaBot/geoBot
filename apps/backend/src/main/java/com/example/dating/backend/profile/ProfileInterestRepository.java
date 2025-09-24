package com.example.dating.backend.profile;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileInterestRepository extends JpaRepository<ProfileInterestEntity, Long> {
  List<ProfileInterestEntity> findByUserId(Long userId);

  void deleteByUserId(Long userId);

  List<ProfileInterestEntity> findByUserIdIn(Collection<Long> userIds);
}
