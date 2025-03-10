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
    WHERE is_active = true 
    AND telegram_id <> :currentUserId
    AND latitude IS NOT NULL 
    AND longitude IS NOT NULL 
    AND (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * 
    cos(radians(longitude) - radians(:lon)) + sin(radians(:lat)) * 
    sin(radians(latitude)))) <= :radius
    AND (:minAge IS NULL OR age >= :minAge)
    AND (:maxAge IS NULL OR age <= :maxAge)
    AND (:gender IS NULL OR gender = :gender OR :gender = 'any')
    """, nativeQuery = true)
  List<User> findUsersNearbyWithFilters(
      @Param("lat") double lat, 
      @Param("lon") double lon, 
      @Param("radius") double radius,
      @Param("currentUserId") Long currentUserId,
      @Param("minAge") Integer minAge,
      @Param("maxAge") Integer maxAge,
      @Param("gender") String gender);

  @Query("SELECT u FROM User u WHERE u.active = TRUE AND u.deactivateAt < :now")
  List<User> findExpiredUsers(@Param("now") LocalDateTime now);
}