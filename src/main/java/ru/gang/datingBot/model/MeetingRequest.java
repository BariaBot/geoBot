package ru.gang.datingBot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "meeting_requests")
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
  
  // Добавлен метод для проверки, есть ли фото в запросе
  public boolean hasPhoto() {
    return photoFileId != null && !photoFileId.isEmpty();
  }
}
