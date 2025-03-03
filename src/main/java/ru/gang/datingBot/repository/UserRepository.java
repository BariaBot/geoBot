package ru.gang.datingBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gang.datingBot.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByTelegramId(Long telegramId);

  @Query(value = """
    SELECT * FROM users 
    WHERE is_active = 't' 
    AND latitude IS NOT NULL 
    AND longitude IS NOT NULL 
    AND (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * 
    cos(radians(longitude) - radians(:lon)) + sin(radians(:lat)) * 
    sin(radians(latitude)))) < :radius
    """, nativeQuery = true)
  List<User> findUsersNearby(@Param("lat") double lat, @Param("lon") double lon, @Param("radius") double radius);

  @Query("SELECT u FROM User u WHERE u.active = TRUE AND u.deactivateAt < :now")
  List<User> findExpiredUsers(@Param("now") LocalDateTime now);
}
