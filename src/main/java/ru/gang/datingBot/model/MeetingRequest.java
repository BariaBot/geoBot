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
  private String status = "pending"; // pending, accepted, declined
  private LocalDateTime scheduledTime;
  private LocalDateTime createdAt = LocalDateTime.now();
}
