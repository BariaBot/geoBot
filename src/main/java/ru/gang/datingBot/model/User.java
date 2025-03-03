package ru.gang.datingBot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private Long telegramId;

  private String username;
  private String gender;
  private Double latitude;
  private Double longitude;
  @Column(name = "is_active")
  private Boolean active = false;
  private Integer searchRadius = 5;
  private LocalDateTime lastActive = LocalDateTime.now();
  private LocalDateTime deactivateAt;

}
