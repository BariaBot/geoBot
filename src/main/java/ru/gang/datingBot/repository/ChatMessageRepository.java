package ru.gang.datingBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gang.datingBot.model.ChatMessage;
import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.User;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
  
  // Получить все сообщения для заданного запроса на встречу
  List<ChatMessage> findByMeetingRequestOrderBySentAtAsc(MeetingRequest meetingRequest);
  
  // Получить все непрочитанные сообщения конкретного пользователя
  List<ChatMessage> findByReceiverAndIsReadFalse(User receiver);
  
  // Получить все активные чаты пользователя
  @Query("SELECT DISTINCT m.meetingRequest FROM ChatMessage m WHERE m.sender = :user OR m.receiver = :user")
  List<MeetingRequest> findActiveChatsByUser(@Param("user") User user);
}