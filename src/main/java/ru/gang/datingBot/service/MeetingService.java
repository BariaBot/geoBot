package ru.gang.datingBot.service;

import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.MeetingRepository;
import ru.gang.datingBot.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MeetingService {
  private final MeetingRepository meetingRepository;
  private final UserRepository userRepository;

  public MeetingService(MeetingRepository meetingRepository, UserRepository userRepository) {
    this.meetingRepository = meetingRepository;
    this.userRepository = userRepository;
  }

  // Отправка запроса на встречу
  public MeetingRequest sendMeetingRequest(Long senderId, Long receiverId, String message, LocalDateTime scheduledTime) {
    User sender = userRepository.findByTelegramId(senderId)
        .orElseThrow(() -> new IllegalArgumentException("Отправитель не найден: " + senderId));
    User receiver = userRepository.findByTelegramId(receiverId)
        .orElseThrow(() -> new IllegalArgumentException("Получатель не найден: " + receiverId));

    MeetingRequest request = new MeetingRequest();
    request.setSender(sender);
    request.setReceiver(receiver);
    request.setMessage(message);
    request.setScheduledTime(scheduledTime);
    request.setStatus("pending");

    return meetingRepository.save(request);
  }

  // Получить все ожидающие запросы для пользователя
  public List<MeetingRequest> getPendingRequestsForUser(Long receiverId) {
    User receiver = userRepository.findByTelegramId(receiverId)
        .orElseThrow(() -> new IllegalArgumentException("Получатель не найден"));
    return meetingRepository.findByReceiverAndStatus(receiver, "pending");
  }

  // Принятие запроса на встречу
  public void acceptMeetingRequest(Long requestId) {
    MeetingRequest request = meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));
    request.setStatus("accepted");
    meetingRepository.save(request);
  }

  // Отклонение запроса на встречу
  public void declineMeetingRequest(Long requestId) {
    MeetingRequest request = meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));
    request.setStatus("declined");
    meetingRepository.save(request);
  }

  // Завершение встречи (бот отправляет запрос на отзыв)
  public void completeMeeting(Long requestId) {
    MeetingRequest request = meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));
    request.setStatus("completed");
    meetingRepository.save(request);

    // Отправка запроса на отзыв
    sendFeedbackRequest(request);
  }

  // Отправка запроса на отзыв после встречи
  private void sendFeedbackRequest(MeetingRequest request) {
    Long senderId = request.getSender().getTelegramId();
    Long receiverId = request.getReceiver().getTelegramId();

    System.out.println("Запрос на отзыв отправлен пользователям: " + senderId + " и " + receiverId);
  }

  // Автоматическая отправка запросов на отзыв (каждый час)
  @Scheduled(fixedRate = 3600000) // Каждые 60 минут
  public void sendMeetingFeedbackRequests() {
    List<MeetingRequest> pastMeetings = meetingRepository.findPastMeetings(LocalDateTime.now().minusHours(1));

    for (MeetingRequest meeting : pastMeetings) {
      sendFeedbackRequest(meeting);
    }
  }
}
