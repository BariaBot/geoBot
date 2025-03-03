package ru.gang.datingBot.service;

import lombok.Getter;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Getter
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User getUserByTelegramId(Long telegramId) {
    return userRepository.findByTelegramId(telegramId).orElse(null);
  }

  @Transactional
  public void updateUserLocation(Long telegramId, double lat, double lon, int hours, int radius,
      String telegramUsername, String firstName, String lastName, String phoneNumber) {
    User user = userRepository.findByTelegramId(telegramId).orElse(null);

    if (user == null) {
      user = new User();
      user.setTelegramId(telegramId);
      user.setActive(true);
    }

    // Обновляем username - сохраняем только реальный username из Telegram
    if (telegramUsername != null && !telegramUsername.isEmpty()) {
      user.setUsername(telegramUsername);
    }
    // Не устанавливаем значение по умолчанию, если username отсутствует

    // Обновляем имя и фамилию, если пришли непустые значения
    if (firstName != null && !firstName.isEmpty()) {
      user.setFirstName(firstName);
    }
    if (lastName != null && !lastName.isEmpty()) {
      user.setLastName(lastName);
    }

    // Обновляем номер телефона, если пришел непустой
    if (phoneNumber != null && !phoneNumber.isEmpty()) {
      user.setPhoneNumber(phoneNumber);
    }

    user.setLatitude(lat);
    user.setLongitude(lon);
    user.setSearchRadius(radius);
    user.setLastActive(LocalDateTime.now());
    user.setDeactivateAt(LocalDateTime.now().plusHours(hours));

    // Ensure profileCompleted is initialized
    if (user.getProfileCompleted() == null) {
      user.setProfileCompleted(false);
    }

    userRepository.save(user);

    System.out.println("✅ Обновлено местоположение для: " + telegramId +
        " (lat: " + lat + ", lon: " + lon + 
        ", username: " + (user.getUsername() != null ? user.getUsername() : "не установлен") +
        ", firstName: " + user.getFirstName() + 
        ", lastName: " + user.getLastName() + 
        ", phone: " + user.getPhoneNumber() + ")");
  }

  @Scheduled(fixedRate = 600000) // Каждые 10 минут
  public void deactivateExpiredUsers() {
    List<User> expiredUsers = userRepository.findExpiredUsers(LocalDateTime.now());
    for (User user : expiredUsers) {
      user.setActive(false);
      userRepository.save(user);
    }
  }

  // Метод поиска пользователей в радиусе
  public List<User> findNearbyUsers(double lat, double lon, int radius) {
    return userRepository.findUsersNearby(lat, lon, radius);
  }

  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  @Transactional
  public void deactivateUser(Long telegramId) {
    User user = userRepository.findByTelegramId(telegramId)
        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    user.setActive(false);
    userRepository.save(user);
  }
  
  // New profile management methods
  
  /**
   * Updates the user's profile description
   */
  @Transactional
  public void updateUserDescription(Long telegramId, String description) {
    User user = getUserOrCreate(telegramId);
    user.setDescription(description);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  /**
   * Updates the user's interests
   */
  @Transactional
  public void updateUserInterests(Long telegramId, String interests) {
    User user = getUserOrCreate(telegramId);
    user.setInterests(interests);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  /**
   * Updates the user's profile photo by storing the Telegram file ID
   */
  @Transactional
  public void updateUserPhoto(Long telegramId, String photoFileId) {
    User user = getUserOrCreate(telegramId);
    user.setPhotoFileId(photoFileId);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  /**
   * Checks and updates the profile completion status
   */
  private void updateProfileCompleteness(User user) {
    try {
      boolean isComplete = 
          user.getFirstName() != null && !user.getFirstName().isEmpty() &&
          user.getDescription() != null && !user.getDescription().isEmpty() &&
          user.getInterests() != null && !user.getInterests().isEmpty() &&
          user.getPhotoFileId() != null && !user.getPhotoFileId().isEmpty();
      
      user.setProfileCompleted(isComplete);
    } catch (Exception e) {
      // Handle potential issues with profileCompleted during migration
      user.setProfileCompleted(false);
      System.err.println("Error updating profile completeness: " + e.getMessage());
    }
  }
  
  /**
   * Returns profile completion percentage
   */
  public int getProfileCompletionPercentage(Long telegramId) {
    User user = getUserOrCreate(telegramId);
    return user.getProfileCompletionPercentage();
  }
  
  /**
   * Gets a user by telegramId or creates a new one if not found
   */
  @Transactional
  private User getUserOrCreate(Long telegramId) {
    User user = userRepository.findByTelegramId(telegramId).orElse(null);
    if (user == null) {
      user = new User();
      user.setTelegramId(telegramId);
      user.setActive(false);
      user.setProfileCompleted(false);
      userRepository.save(user);
    } else if (user.getProfileCompleted() == null) {
      // Ensure profileCompleted is initialized during transition
      user.setProfileCompleted(false);
      userRepository.save(user);
    }
    return user;
  }
  
  /**
   * Gets formatted profile information as a string
   */
  public String getUserProfileInfo(Long telegramId) {
    User user = getUserOrCreate(telegramId);
    return user.getProfileInfo();
  }
}
