package ru.gang.datingBot.repository;

import java.util.Optional;
import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface MeetingRepository extends JpaRepository<MeetingRequest, Long> {

  // Получить все ожидающие запросы для пользователя
  List<MeetingRequest> findByReceiverAndStatus(User receiver, String status);

  // Найти запросы, отправленные конкретным пользователем
  List<MeetingRequest> findBySender(User sender);

  // Найти запросы, полученные конкретным пользователем
  List<MeetingRequest> findByReceiver(User receiver);

  // Проверить, есть ли активный запрос между двумя пользователями
  @Query("SELECT m FROM MeetingRequest m WHERE m.sender = :sender AND m.receiver = :receiver AND m.status = 'pending'")
  Optional<MeetingRequest> findActiveRequestBetweenUsers(@Param("sender") User sender, @Param("receiver") User receiver);
}
