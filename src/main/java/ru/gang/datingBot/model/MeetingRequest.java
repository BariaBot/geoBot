package ru.gang.datingBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
  private String status = "pending";
  
  @Column(nullable = true)
  private String photoFileId;
  
  private LocalDateTime scheduledTime;
  private LocalDateTime createdAt = LocalDateTime.now();
  
  public boolean hasPhoto() {
    return photoFileId != null && !photoFileId.isEmpty();
  }
}