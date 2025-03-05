package ru.gang.datingBot.service;

import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.MeetingRepository;
import ru.gang.datingBot.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeetingService {
  private final MeetingRepository meetingRepository;
  private final UserRepository userRepository;

  public MeetingService(MeetingRepository meetingRepository, UserRepository userRepository) {
    this.meetingRepository = meetingRepository;
    this.userRepository = userRepository;
  }

  // Отправка запроса на встречу без фото
  public MeetingRequest sendMeetingRequest(Long senderId, Long receiverId, String message, LocalDateTime scheduledTime) {
    return sendMeetingRequest(senderId, receiverId, message, scheduledTime, null);
  }
  
  // Отправка запроса на встречу с фото
  public MeetingRequest sendMeetingRequest(Long senderId, Long receiverId, String message, LocalDateTime scheduledTime, String photoFileId) {
    System.out.println("DEBUG: Создание запроса на встречу от " + senderId + " к " + receiverId);
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
    
    // Добавляем фото, если передано
    if (photoFileId != null && !photoFileId.isEmpty()) {
      request.setPhotoFileId(photoFileId);
    }

    System.out.println("DEBUG: Сохранение запроса на встречу в базу данных");
    MeetingRequest savedRequest = meetingRepository.save(request);
    System.out.println("DEBUG: Запрос на встречу сохранен с ID " + savedRequest.getId());
    
    return savedRequest;
  }

  // Получить все ожидающие запросы для пользователя
  public List<MeetingRequest> getPendingRequestsForUser(Long receiverId) {
    User receiver = userRepository.findByTelegramId(receiverId)
        .orElseThrow(() -> new IllegalArgumentException("Получатель не найден"));
    return meetingRepository.findByReceiverAndStatus(receiver, "pending");
  }
  
  // Получить все принятые запросы для пользователя (как отправителя, так и получателя)
  public List<MeetingRequest> getAcceptedRequestsForUser(Long userId) {
    User user = userRepository.findByTelegramId(userId)
        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
    
    // Получаем все запросы, где пользователь является отправителем или получателем
    List<MeetingRequest> allRequests = meetingRepository.findAll();
    
    return allRequests.stream()
            .filter(request -> "accepted".equals(request.getStatus()))
            .filter(request -> request.getSender().getTelegramId().equals(userId) || 
                             request.getReceiver().getTelegramId().equals(userId))
            .collect(Collectors.toList());
  }

  // Принятие запроса на встречу
  public void acceptMeetingRequest(Long requestId) {
    MeetingRequest request = meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));
    request.setStatus("accepted");
    meetingRepository.save(request);
    System.out.println("DEBUG: Запрос на встречу с ID " + requestId + " принят");
  }

  // Отклонение запроса на встречу
  public void declineMeetingRequest(Long requestId) {
    MeetingRequest request = meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));
    request.setStatus("declined");
    meetingRepository.save(request);
    System.out.println("DEBUG: Запрос на встречу с ID " + requestId + " отклонен");
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
  
  // Получить запрос по ID
  public MeetingRequest getMeetingRequestById(Long requestId) {
    return meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос на встречу не найден"));
  }
}