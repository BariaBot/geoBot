package ru.gang.datingBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}