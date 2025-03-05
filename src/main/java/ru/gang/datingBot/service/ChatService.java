package ru.gang.datingBot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gang.datingBot.model.ChatMessage;
import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.ChatMessageRepository;
import ru.gang.datingBot.repository.MeetingRepository;
import ru.gang.datingBot.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ChatService {
  private final ChatMessageRepository chatMessageRepository;
  private final MeetingRepository meetingRepository;
  private final UserRepository userRepository;

  public ChatService(ChatMessageRepository chatMessageRepository, 
                     MeetingRepository meetingRepository,
                     UserRepository userRepository) {
    this.chatMessageRepository = chatMessageRepository;
    this.meetingRepository = meetingRepository;
    this.userRepository = userRepository;
  }

  /**
   * Отправить сообщение
   * @param senderId ID отправителя
   * @param receiverId ID получателя
   * @param meetingRequestId ID запроса на встречу
   * @param text Текст сообщения (для медиа может содержать тип контента)
   * @param mediaFileId ID файла медиа (если есть)
   * @return Сохраненное сообщение
   */
  @Transactional
  public ChatMessage sendMessage(Long senderId, Long receiverId, Long meetingRequestId, String text, String mediaFileId) {
    User sender = userRepository.findByTelegramId(senderId)
        .orElseThrow(() -> new IllegalArgumentException("Отправитель не найден: " + senderId));
    User receiver = userRepository.findByTelegramId(receiverId)
        .orElseThrow(() -> new IllegalArgumentException("Получатель не найден: " + receiverId));
    MeetingRequest meetingRequest = meetingRepository.findById(meetingRequestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос на встречу не найден: " + meetingRequestId));
    
    // Проверка, что встреча принята
    if (!"accepted".equals(meetingRequest.getStatus())) {
      throw new IllegalStateException("Чат невозможен: запрос на встречу не был принят");
    }
    
    // Проверка, что пользователи связаны с этим запросом
    if (!meetingRequest.getSender().equals(sender) && !meetingRequest.getReceiver().equals(sender)) {
      throw new IllegalArgumentException("Отправитель не связан с данным запросом на встречу");
    }
    if (!meetingRequest.getSender().equals(receiver) && !meetingRequest.getReceiver().equals(receiver)) {
      throw new IllegalArgumentException("Получатель не связан с данным запросом на встречу");
    }
    
    ChatMessage message = new ChatMessage();
    message.setSender(sender);
    message.setReceiver(receiver);
    message.setMeetingRequest(meetingRequest);
    message.setMessageText(text);
    
    // Если есть ID медиа-файла, сохраняем его
    if (mediaFileId != null && !mediaFileId.isEmpty()) {
      message.setPhotoFileId(mediaFileId);
    }
    
    return chatMessageRepository.save(message);
  }
  
  /**
   * Получить историю чата
   */
  public List<ChatMessage> getChatHistory(Long meetingRequestId) {
    MeetingRequest meetingRequest = meetingRepository.findById(meetingRequestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос на встречу не найден: " + meetingRequestId));
    
    return chatMessageRepository.findByMeetingRequestOrderBySentAtAsc(meetingRequest);
  }
  
  /**
   * Пометить сообщения как прочитанные
   */
  @Transactional
  public void markMessagesAsRead(Long userId, Long meetingRequestId) {
    User user = userRepository.findByTelegramId(userId)
        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));
    MeetingRequest meetingRequest = meetingRepository.findById(meetingRequestId)
        .orElseThrow(() -> new IllegalArgumentException("Запрос на встречу не найден: " + meetingRequestId));
    
    List<ChatMessage> messages = chatMessageRepository.findByMeetingRequestOrderBySentAtAsc(meetingRequest);
    
    for (ChatMessage message : messages) {
      if (message.getReceiver().equals(user) && !message.getIsRead()) {
        message.setIsRead(true);
        chatMessageRepository.save(message);
      }
    }
  }
  
  /**
   * Найти активный запрос между пользователями
   */
  public Optional<MeetingRequest> findActiveChatBetweenUsers(Long userId1, Long userId2) {
    User user1 = userRepository.findByTelegramId(userId1)
        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId1));
    User user2 = userRepository.findByTelegramId(userId2)
        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId2));
    
    return meetingRepository.findAll().stream()
        .filter(mr -> "accepted".equals(mr.getStatus()))
        .filter(mr -> (mr.getSender().equals(user1) && mr.getReceiver().equals(user2)) ||
                     (mr.getSender().equals(user2) && mr.getReceiver().equals(user1)))
        .findFirst();
  }
}