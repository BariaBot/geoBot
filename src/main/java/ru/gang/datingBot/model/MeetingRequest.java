package ru.gang.datingBot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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
  
  // Добавляем поля для информации о месте встречи
  @ManyToOne
  @JoinColumn(name = "place_id")
  private Place selectedPlace;
  
  private LocalDateTime meetingTime;
  
  // Поля для подтверждения места встречи
  private boolean senderConfirmed = false;
  private boolean receiverConfirmed = false;
  
  // Поле для отслеживания отправки запроса на обратную связь
  private boolean feedbackSent = false;
  
  // Геттеры и сеттеры
  public Long getId() {
    return id;
  }
  
  public void setId(Long id) {
    this.id = id;
  }
  
  public User getSender() {
    return sender;
  }
  
  public void setSender(User sender) {
    this.sender = sender;
  }
  
  public User getReceiver() {
    return receiver;
  }
  
  public void setReceiver(User receiver) {
    this.receiver = receiver;
  }
  
  public String getMessage() {
    return message;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
  
  public String getStatus() {
    return status;
  }
  
  public void setStatus(String status) {
    this.status = status;
  }
  
  public String getPhotoFileId() {
    return photoFileId;
  }
  
  public void setPhotoFileId(String photoFileId) {
    this.photoFileId = photoFileId;
  }
  
  public LocalDateTime getScheduledTime() {
    return scheduledTime;
  }
  
  public void setScheduledTime(LocalDateTime scheduledTime) {
    this.scheduledTime = scheduledTime;
  }
  
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
  
  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
  
  public Place getSelectedPlace() {
    return selectedPlace;
  }
  
  public void setSelectedPlace(Place selectedPlace) {
    this.selectedPlace = selectedPlace;
  }
  
  public LocalDateTime getMeetingTime() {
    return meetingTime;
  }
  
  public void setMeetingTime(LocalDateTime meetingTime) {
    this.meetingTime = meetingTime;
  }
  
  public boolean isSenderConfirmed() {
    return senderConfirmed;
  }
  
  public void setSenderConfirmed(boolean senderConfirmed) {
    this.senderConfirmed = senderConfirmed;
  }
  
  public boolean isReceiverConfirmed() {
    return receiverConfirmed;
  }
  
  public void setReceiverConfirmed(boolean receiverConfirmed) {
    this.receiverConfirmed = receiverConfirmed;
  }
  
  public boolean getFeedbackSent() {
    return feedbackSent;
  }
  
  public void setFeedbackSent(boolean feedbackSent) {
    this.feedbackSent = feedbackSent;
  }
  
  // Добавлен метод для проверки, есть ли фото в запросе
  public boolean hasPhoto() {
    return photoFileId != null && !photoFileId.isEmpty();
  }
  
  // Проверяет, подтверждено ли место обоими пользователями
  public boolean isPlaceConfirmedByBoth() {
    return senderConfirmed && receiverConfirmed;
  }
}
