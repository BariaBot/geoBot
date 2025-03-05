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
  
  // Добавлены поля возраста и пола
  private Integer age;
  
  @Column(length = 10)
  private String gender; // "male", "female", "other"
  
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
  
  // Поля профиля
  @Column(nullable = true, length = 1000)
  private String description;
  
  @Column(nullable = true, length = 500)
  private String interests;
  
  @Column(nullable = true)
  private String photoFileId; // Telegram file ID для фото профиля
  
  // Добавлены настройки фильтрации для поиска
  @Column(nullable = true)
  private Integer minAgePreference;
  
  @Column(nullable = true)
  private Integer maxAgePreference;
  
  @Column(nullable = true, length = 10)
  private String genderPreference; // "male", "female", "any"
  
  // Индикатор заполненности профиля
  @Column(nullable = false, columnDefinition = "boolean default false")
  private Boolean profileCompleted = false;
  
  // Возвращает отформатированную строку с информацией профиля пользователя
  public String getProfileInfo() {
    StringBuilder profile = new StringBuilder();
    profile.append("📋 Ваш профиль:\n\n");
    
    profile.append("👤 Имя: ").append(firstName != null ? firstName : "Не указано").append("\n");
    profile.append("📛 Фамилия: ").append(lastName != null ? lastName : "Не указано").append("\n");
    profile.append("🔍 Username: ").append(username != null ? "@" + username : "Не указано").append("\n");
    
    // Добавлена информация о возрасте и поле
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
    
    // Добавлены настройки поиска
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
  
  // Возвращает процент заполненности профиля
  public int getProfileCompletionPercentage() {
    int totalFields = 8; // firstName, lastName, username, age, gender, description, interests, photoFileId
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
  
  // Вспомогательные методы для отображения пола
  public String getGenderDisplay() {
    if (gender == null) return "Не указан";
    return switch (gender) {
      case "male" -> "Мужской";
      case "female" -> "Женский";
      case "other" -> "Другой";
      default -> "Не указан";
    };
  }
  
  // Вспомогательные методы для отображения предпочтений по полу
  public String getGenderPreferenceDisplay() {
    if (genderPreference == null) return "Любой";
    return switch (genderPreference) {
      case "male" -> "Мужской";
      case "female" -> "Женский";
      case "any" -> "Любой";
      default -> "Любой";
    };
  }
  
  // Экранирует специальные символы Markdown
  private String escapeMarkdown(String text) {
    if (text == null) return "";
    return text
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("`", "\\`");
  }
}