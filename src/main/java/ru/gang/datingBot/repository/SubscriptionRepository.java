package ru.gang.datingBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gang.datingBot.model.Subscription;
import ru.gang.datingBot.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    List<Subscription> findByUser(User user);
    
    List<Subscription> findByUserAndStatus(User user, String status);
    
    @Query("SELECT s FROM Subscription s WHERE s.user = :user AND s.endDate > :now AND s.status = 'completed' ORDER BY s.endDate DESC")
    List<Subscription> findActiveSubscriptions(@Param("user") User user, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM Subscription s WHERE s.endDate < :now AND s.status = 'completed'")
    List<Subscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);
    
    Optional<Subscription> findTopByUserOrderByEndDateDesc(User user);
}