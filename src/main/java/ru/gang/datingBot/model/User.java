package ru.gang.datingBot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private Long telegramId;

  private String username;
  private Integer age;
  
  @Column(length = 10)
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
  
  @Column(nullable = true, length = 1000)
  private String description;
  
  @Column(nullable = true, length = 500)
  private String interests;
  
  @Column(nullable = true)
  private String photoFileId;
  
  @Column(nullable = true)
  private Integer minAgePreference;
  
  @Column(nullable = true)
  private Integer maxAgePreference;
  
  @Column(nullable = true, length = 10)
  private String genderPreference;
  
  @Column(nullable = false, columnDefinition = "boolean default false")
  private Boolean profileCompleted = false;
  
  @Column(nullable = false, columnDefinition = "boolean default false")
  private Boolean isVip = false;
  
  @Column(nullable = true)
  private LocalDateTime vipExpiresAt;
  
  public String getProfileInfo() {
    StringBuilder profile = new StringBuilder();
    profile.append("📋 Ваш профиль:\n\n");
    
    if (isVip != null && isVip) {
      profile.append("👑 VIP-статус до: ").append(vipExpiresAt.toLocalDate()).append("\n\n");
    }
    
    profile.append("👤 Имя: ").append(firstName != null ? firstName : "Не указано").append("\n");
    profile.append("📛 Фамилия: ").append(lastName != null ? lastName : "Не указано").append("\n");
    profile.append("🔍 Username: ").append(username != null ? "@" + username : "Не указано").append("\n");
    
    profile.append("🎂 Возраст: ").append(age != null ? age : "Не указан").append("\n");
    profile.append("⚧ Пол: ").append(getGenderDisplay()).append("\n");
    
    if (description != null && !description.isEmpty()) {
      profile.append("\n📝 О себе:\n").append(description).append("\n");
    } else {
      profile.append("\n📝 О себе: Не указано\n");
    }
    
    if (interests != null && !interests.isEmpty()) {
      profile.append("\n⭐ Интересы:\n").append(interests).append("\n");
    } else {
      profile.append("\n⭐ Интересы: Не указано\n");
    }
    
    profile.append("\n📱 Телефон: ").append(phoneNumber != null ? phoneNumber : "Не указано").append("\n");
    profile.append("🖼 Фото: ").append(photoFileId != null ? "Загружено" : "Не загружено").append("\n");
    
    profile.append("\n🔍 Настройки поиска:\n");
    String ageRange = "Любой";
    if (minAgePreference != null && maxAgePreference != null) {
      ageRange = minAgePreference + " - " + maxAgePreference + " лет";
    } else if (minAgePreference != null) {
      ageRange = "от " + minAgePreference + " лет";
    } else if (maxAgePreference != null) {
      ageRange = "до " + maxAgePreference + " лет";
    }
    profile.append("🎯 Возраст: ").append(ageRange).append("\n");
    profile.append("👥 Пол: ").append(getGenderPreferenceDisplay()).append("\n");
    
    return profile.toString();
  }
  
  public int getProfileCompletionPercentage() {
    int totalFields = 8;
    int completedFields = 0;
    
    if (firstName != null && !firstName.isEmpty()) completedFields++;
    if (lastName != null && !lastName.isEmpty()) completedFields++;
    if (username != null && !username.isEmpty()) completedFields++;
    if (age != null) completedFields++;
    if (gender != null && !gender.isEmpty()) completedFields++;
    if (description != null && !description.isEmpty()) completedFields++;
    if (interests != null && !interests.isEmpty()) completedFields++;
    if (photoFileId != null && !photoFileId.isEmpty()) completedFields++;
    
    return (completedFields * 100) / totalFields;
  }
  
  public String getGenderDisplay() {
    if (gender == null) return "Не указан";
    return switch (gender) {
      case "male" -> "Мужской";
      case "female" -> "Женский";
      case "other" -> "Другой";
      default -> "Не указан";
    };
  }
  
  public String getGenderPreferenceDisplay() {
    if (genderPreference == null) return "Любой";
    return switch (genderPreference) {
      case "male" -> "Мужской";
      case "female" -> "Женский";
      case "any" -> "Любой";
      default -> "Любой";
    };
  }
  
  public boolean isVipActive() {
    return isVip != null && isVip && vipExpiresAt != null && vipExpiresAt.isAfter(LocalDateTime.now());
  }
}