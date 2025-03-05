package ru.gang.datingBot.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.Place;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.MeetingRepository;
import ru.gang.datingBot.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeetingService {
  private final MeetingRepository meetingRepository;
  private final UserRepository userRepository;
  private final MessageSender messageSender;

  public MeetingService(
          MeetingRepository meetingRepository, 
          UserRepository userRepository, 
          @Lazy MessageSender messageSender) {  // Добавляем @Lazy здесь
    this.meetingRepository = meetingRepository;
    this.userRepository = userRepository;
    this.messageSender = messageSender;
  }

  /**
   * Отправка запроса на встречу без фото
   */
  public MeetingRequest sendMeetingRequest(Long senderId, Long receiverId, String message, LocalDateTime scheduledTime) {
    return sendMeetingRequest(senderId, receiverId, message, scheduledTime, null);
  }
  
  /**
   * Отправка запроса на встречу с фото
   */
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

  /**
   * Получить все ожидающие запросы для пользователя
   */
  public List<MeetingRequest> getPendingRequestsForUser(Long receiverId) {
    User receiver = userRepository.findByTelegramId(receiverId)
        .orElseThrow(() -> new IllegalArgumentException("Получатель не найден"));
    return meetingRepository.findByReceiverAndStatus(receiver, "pending");
  }
  
  /**
   * Получить все принятые запросы для пользователя (как отправителя, так и получателя)
   */
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

  /**
   * Принятие запроса на встречу
   */
  public void acceptMeetingRequest(Long requestId) {
    MeetingRequest request = meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));
    request.setStatus("accepted");
    meetingRepository.save(request);
    System.out.println("DEBUG: Запрос на встречу с ID " + requestId + " принят");
  }

  /**
   * Отклонение запроса на встречу
   */
  public void declineMeetingRequest(Long requestId) {
    MeetingRequest request = meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));
    request.setStatus("declined");
    meetingRepository.save(request);
    System.out.println("DEBUG: Запрос на встречу с ID " + requestId + " отклонен");
  }

  /**
   * Завершение встречи (бот отправляет запрос на отзыв)
   */
  public void completeMeeting(Long requestId) {
    MeetingRequest request = meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос не найден"));
    request.setStatus("completed");
    meetingRepository.save(request);

    // Отправка запроса на отзыв
    sendFeedbackRequest(request);
  }

  /**
   * Отправка запроса на отзыв после встречи
   */
  private void sendFeedbackRequest(MeetingRequest request) {
    if (request.getSelectedPlace() == null) {
      return;
    }
    
    String message = "👋 Как прошла ваша встреча в " + request.getSelectedPlace().getName() + "? " +
            "Надеемся, все прошло хорошо! Если захотите поделиться впечатлениями, используйте команду /feedback";
    
    // Отправляем сообщение обоим участникам
    messageSender.sendTextMessage(request.getSender().getTelegramId(), message);
    messageSender.sendTextMessage(request.getReceiver().getTelegramId(), message);
    
    System.out.println("Запрос на обратную связь отправлен пользователям: " + 
            request.getSender().getTelegramId() + " и " + 
            request.getReceiver().getTelegramId());
  }

  /**
   * Автоматическая отправка запросов на отзыв (каждый час)
   */
  @Scheduled(fixedRate = 3600000) // Каждые 60 минут
  public void sendMeetingFeedbackRequests() {
    List<MeetingRequest> pastMeetings = meetingRepository.findPastMeetings(LocalDateTime.now().minusHours(1));

    for (MeetingRequest meeting : pastMeetings) {
      sendFeedbackRequest(meeting);
    }
  }
  
  /**
   * Получить запрос по ID
   */
  public MeetingRequest getMeetingRequestById(Long requestId) {
    return meetingRepository.findById(requestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос на встречу не найден"));
  }

  /**
   * Обновляет информацию о месте встречи и времени для запроса на встречу
   * @return true если успешно, false если произошла ошибка
   */
  @Transactional
  public boolean updateMeetingRequestWithPlace(Long requestId, Place place, LocalDateTime meetingTime, Long initiatorId) {
    try {
        MeetingRequest request = meetingRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Запрос на встречу не найден: " + requestId));
        
        request.setSelectedPlace(place);
        request.setMeetingTime(meetingTime);
        request.setFeedbackSent(false);
        
        // Устанавливаем флаг подтверждения для инициатора
        if (request.getSender().getTelegramId().equals(initiatorId)) {
            request.setSenderConfirmed(true);
            request.setReceiverConfirmed(false);
        } else if (request.getReceiver().getTelegramId().equals(initiatorId)) {
            request.setReceiverConfirmed(true);
            request.setSenderConfirmed(false);
        } else {
            return false; // Инициатор не является участником встречи
        }
        
        meetingRepository.save(request);
        return true;
    } catch (Exception e) {
        System.out.println("Ошибка при обновлении запроса на встречу: " + e.getMessage());
        return false;
    }
  }

  /**
   * Подтверждает встречу пользователем
   * @return true если успешно, false если произошла ошибка
   */
  @Transactional
  public boolean confirmMeetingByUser(Long requestId, Long userId) {
    try {
        MeetingRequest request = meetingRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Запрос на встречу не найден: " + requestId));
        
        // Устанавливаем флаг подтверждения в зависимости от того, кто подтверждает
        if (request.getSender().getTelegramId().equals(userId)) {
            request.setSenderConfirmed(true);
        } else if (request.getReceiver().getTelegramId().equals(userId)) {
            request.setReceiverConfirmed(true);
        } else {
            return false; // Пользователь не является участником встречи
        }
        
        meetingRepository.save(request);
        return true;
    } catch (Exception e) {
        System.out.println("Ошибка при подтверждении встречи: " + e.getMessage());
        return false;
    }
  }

  /**
   * Находит встречи, для которых нужно отправить запрос на обратную связь
   */
  public List<MeetingRequest> findMeetingsForFeedback(LocalDateTime startTime, LocalDateTime endTime) {
    return meetingRepository.findAll().stream()
            .filter(meeting -> "accepted".equals(meeting.getStatus()))
            .filter(meeting -> meeting.isPlaceConfirmedByBoth())
            .filter(meeting -> meeting.getMeetingTime() != null)
            .filter(meeting -> meeting.getMeetingTime().isAfter(startTime) && 
                               meeting.getMeetingTime().isBefore(endTime))
            .filter(meeting -> !meeting.isFeedbackSent())
            .collect(Collectors.toList());
  }

  /**
   * Отправляет запрос на обратную связь после встречи
   */
  @Scheduled(fixedRate = 300000) // Каждые 5 минут
  public void checkAndSendFeedbackRequests() {
    LocalDateTime now = LocalDateTime.now();
    List<MeetingRequest> meetingsToCheck = findMeetingsForFeedback(now.minusHours(1), now);
    
    for (MeetingRequest meeting : meetingsToCheck) {
        sendFeedbackRequest(meeting);
        meeting.setFeedbackSent(true);
        meetingRepository.save(meeting);
    }
  }
}