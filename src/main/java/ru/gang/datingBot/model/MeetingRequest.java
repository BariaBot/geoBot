package ru.gang.datingBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_requests")
@Getter
@Setter
public class MeetingRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @ManyToOne
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  private String message;
  private String status = "pending"; // pending, accepted, declined, completed
  
  // Добавляем поле для хранения ID фотографии Telegram
  @Column(nullable = true)
  private String photoFileId;
  
  private LocalDateTime scheduledTime;
  private LocalDateTime createdAt = LocalDateTime.now();
  
  // Добавляем поля для информации о месте встречи
  @ManyToOne
  @JoinColumn(name = "place_id")
  private Place selectedPlace;
  
  private LocalDateTime meetingTime;
  
  // Поля для подтверждения места встречи
  private boolean senderConfirmed = false;
  private boolean receiverConfirmed = false;
  
  // Поле для отслеживания отправки запроса на обратную связь
  @Column(name = "feedback_sent")
  private boolean feedbackSent = false;
  
  // Добавлен метод для проверки, есть ли фото в запросе
  public boolean hasPhoto() {
    return photoFileId != null && !photoFileId.isEmpty();
  }
  
  // Проверяет, подтверждено ли место обоими пользователями
  public boolean isPlaceConfirmedByBoth() {
    return senderConfirmed && receiverConfirmed;
  }
}