package ru.gang.datingBot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users", schema = "public")
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
  
  // Геттеры и сеттеры
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTelegramId() {
    return telegramId;
  }

  public void setTelegramId(Long telegramId) {
    this.telegramId = telegramId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Integer getSearchRadius() {
    return searchRadius;
  }

  public void setSearchRadius(Integer searchRadius) {
    this.searchRadius = searchRadius;
  }

  public LocalDateTime getLastActive() {
    return lastActive;
  }

  public void setLastActive(LocalDateTime lastActive) {
    this.lastActive = lastActive;
  }

  public LocalDateTime getDeactivateAt() {
    return deactivateAt;
  }

  public void setDeactivateAt(LocalDateTime deactivateAt) {
    this.deactivateAt = deactivateAt;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getInterests() {
    return interests;
  }

  public void setInterests(String interests) {
    this.interests = interests;
  }

  public String getPhotoFileId() {
    return photoFileId;
  }

  public void setPhotoFileId(String photoFileId) {
    this.photoFileId = photoFileId;
  }

  public Integer getMinAgePreference() {
    return minAgePreference;
  }

  public void setMinAgePreference(Integer minAgePreference) {
    this.minAgePreference = minAgePreference;
  }

  public Integer getMaxAgePreference() {
    return maxAgePreference;
  }

  public void setMaxAgePreference(Integer maxAgePreference) {
    this.maxAgePreference = maxAgePreference;
  }

  public String getGenderPreference() {
    return genderPreference;
  }

  public void setGenderPreference(String genderPreference) {
    this.genderPreference = genderPreference;
  }

  public Boolean getProfileCompleted() {
    return profileCompleted;
  }

  public void setProfileCompleted(Boolean profileCompleted) {
    this.profileCompleted = profileCompleted;
  }
  
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