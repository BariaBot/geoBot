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
  
  @Column(nullable = true)
  private String firstName;

  @Column(nullable = true)
  private String lastName;

  @Column(nullable = true, unique = true)
  private String phoneNumber;
  
  // New profile fields - ensure they're nullable
  @Column(nullable = true, length = 1000)
  private String description;
  
  @Column(nullable = true, length = 500)
  private String interests;
  
  @Column(nullable = true)
  private String photoFileId; // Telegram file ID for the profile photo
  
  // Profile completeness indicator with default value
  @Column(nullable = false, columnDefinition = "boolean default false")
  private Boolean profileCompleted = false;
  
  // Returns a formatted string with user's profile information
  public String getProfileInfo() {
    StringBuilder profile = new StringBuilder();
    profile.append("📋 *Ваш профиль:*\n\n");
    
    profile.append("👤 *Имя:* ").append(firstName != null ? firstName : "Не указано").append("\n");
    profile.append("📛 *Фамилия:* ").append(lastName != null ? lastName : "Не указано").append("\n");
    profile.append("🔍 *Username:* ").append(username != null ? "@" + username : "Не указано").append("\n");
    
    if (description != null && !description.isEmpty()) {
      profile.append("\n📝 *О себе:*\n").append(description).append("\n");
    } else {
      profile.append("\n📝 *О себе:* Не указано\n");
    }
    
    if (interests != null && !interests.isEmpty()) {
      profile.append("\n⭐ *Интересы:*\n").append(interests).append("\n");
    } else {
      profile.append("\n⭐ *Интересы:* Не указано\n");
    }
    
    profile.append("\n📱 *Телефон:* ").append(phoneNumber != null ? phoneNumber : "Не указано").append("\n");
    profile.append("🖼 *Фото:* ").append(photoFileId != null ? "Загружено" : "Не загружено").append("\n");
    
    return profile.toString();
  }
  
  // Returns the percentage of profile completion
  public int getProfileCompletionPercentage() {
    int totalFields = 6; // firstName, lastName, username, description, interests, photoFileId
    int completedFields = 0;
    
    if (firstName != null && !firstName.isEmpty()) completedFields++;
    if (lastName != null && !lastName.isEmpty()) completedFields++;
    if (username != null && !username.isEmpty()) completedFields++;
    if (description != null && !description.isEmpty()) completedFields++;
    if (interests != null && !interests.isEmpty()) completedFields++;
    if (photoFileId != null && !photoFileId.isEmpty()) completedFields++;
    
    return (completedFields * 100) / totalFields;
  }
}
