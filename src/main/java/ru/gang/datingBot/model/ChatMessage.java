package ru.gang.datingBot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @ManyToOne
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  @ManyToOne
  @JoinColumn(name = "meeting_request_id", nullable = false)
  private MeetingRequest meetingRequest;

  @Column(nullable = false, length = 2000)
  private String messageText;

  @Column(nullable = true)
  private String photoFileId;

  private LocalDateTime sentAt = LocalDateTime.now();
  
  private Boolean isRead = false;
  
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

  public MeetingRequest getMeetingRequest() {
    return meetingRequest;
  }

  public void setMeetingRequest(MeetingRequest meetingRequest) {
    this.meetingRequest = meetingRequest;
  }

  public String getMessageText() {
    return messageText;
  }

  public void setMessageText(String messageText) {
    this.messageText = messageText;
  }

  public String getPhotoFileId() {
    return photoFileId;
  }

  public void setPhotoFileId(String photoFileId) {
    this.photoFileId = photoFileId;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(LocalDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public Boolean getIsRead() {
    return isRead;
  }

  public void setIsRead(Boolean isRead) {
    this.isRead = isRead;
  }
}