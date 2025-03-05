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
    }

    // Самое важное - гарантируем, что пользователь активен
    user.setActive(true);

    // Обновляем username - сохраняем только реальный username из Telegram
    if (telegramUsername != null && !telegramUsername.isEmpty()) {
      user.setUsername(telegramUsername);
    }

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

    // Убедимся, что profileCompleted инициализирован
    if (user.getProfileCompleted() == null) {
      user.setProfileCompleted(false);
    }

    userRepository.save(user);

    System.out.println("✅ Обновлено местоположение для: " + telegramId +
        " (lat: " + lat + ", lon: " + lon + 
        ", username: " + (user.getUsername() != null ? user.getUsername() : "не установлен") +
        ", firstName: " + user.getFirstName() + 
        ", lastName: " + user.getLastName() + 
        ", phone: " + user.getPhoneNumber() + 
        ", active: " + user.getActive() + ")");
  }

  @Scheduled(fixedRate = 600000) // Каждые 10 минут
  public void deactivateExpiredUsers() {
    System.out.println("Проверка активности пользователей...");
    // Увеличиваем срок действия для истекших пользователей вместо их деактивации
    List<User> expiredUsers = userRepository.findExpiredUsers(LocalDateTime.now());
    System.out.println("Найдено " + expiredUsers.size() + " пользователей с истекшим временем активности");
    for (User user : expiredUsers) {
      // Вместо деактивации продлеваем активность
      user.setDeactivateAt(LocalDateTime.now().plusDays(30));
      userRepository.save(user);
      System.out.println("Продлена активность пользователя " + user.getTelegramId() + " на 30 дней");
    }
  }

  // Метод поиска пользователей в радиусе с применением фильтров
  public List<User> findNearbyUsers(Long currentUserId, double lat, double lon, int radius) {
    User currentUser = getUserByTelegramId(currentUserId);
    Integer minAge = null;
    Integer maxAge = null;
    String gender = null;

    // Проверяем наличие текущего пользователя и его настроек
    if (currentUser != null) {
      minAge = currentUser.getMinAgePreference();
      maxAge = currentUser.getMaxAgePreference();

      // Определяем значение для фильтра по полу
      if (currentUser.getGenderPreference() != null && !currentUser.getGenderPreference().equals("any")) {
        gender = currentUser.getGenderPreference();
      }
    }

    List<User> users = userRepository.findUsersNearbyWithFilters(lat, lon, radius, currentUserId, minAge, maxAge, gender);

    System.out.println("Найдено пользователей: " + (users != null ? users.size() : 0));
    if (users != null && !users.isEmpty()) {
      for (User user : users) {
        System.out.println(" - " + user.getTelegramId() + " | " +
            (user.getUsername() != null ? user.getUsername() : "без username") +
            " | " + (user.getFirstName() != null ? user.getFirstName() : "") +
            " | lat: " + user.getLatitude() + " lon: " + user.getLongitude() +
            " | active: " + user.getActive());
      }
    }

    return users;
  }
  
  // Метод для фильтрации списка пользователей по общим интересам
  public List<User> filterUsersByCommonInterests(List<User> users, Long currentUserId) {
    User currentUser = getUserByTelegramId(currentUserId);
    if (currentUser.getInterests() == null || currentUser.getInterests().isEmpty()) {
      return users; // Если у текущего пользователя нет интересов, возвращаем исходный список
    }
    
    // Можно добавить более продвинутую логику для поиска общих интересов
    // Например, разделить на ключевые слова и искать совпадения
    return users.stream()
        .filter(user -> user.getInterests() != null && 
                       (user.getInterests().toLowerCase().contains(currentUser.getInterests().toLowerCase()) ||
                        currentUser.getInterests().toLowerCase().contains(user.getInterests().toLowerCase())))
        .toList();
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
    System.out.println("DEBUG: Пользователь " + telegramId + " деактивирован вручную");
  }
  
  // Методы управления профилем
  
  /**
   * Обновляет описание профиля пользователя
   */
  @Transactional
  public void updateUserDescription(Long telegramId, String description) {
    User user = getUserOrCreate(telegramId);
    user.setDescription(description);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  /**
   * Обновляет интересы пользователя
   */
  @Transactional
  public void updateUserInterests(Long telegramId, String interests) {
    User user = getUserOrCreate(telegramId);
    user.setInterests(interests);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  /**
   * Обновляет фото профиля пользователя, сохраняя ID файла в Telegram
   */
  @Transactional
  public void updateUserPhoto(Long telegramId, String photoFileId) {
    User user = getUserOrCreate(telegramId);
    user.setPhotoFileId(photoFileId);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  /**
   * Обновляет возраст пользователя
   */
  @Transactional
  public void updateUserAge(Long telegramId, Integer age) {
    User user = getUserOrCreate(telegramId);
    user.setAge(age);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  /**
   * Обновляет пол пользователя
   */
  @Transactional
  public void updateUserGender(Long telegramId, String gender) {
    User user = getUserOrCreate(telegramId);
    user.setGender(gender);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  /**
   * Обновляет настройки поиска пользователя
   */
  @Transactional
  public void updateUserSearchPreferences(Long telegramId, Integer minAge, Integer maxAge, String genderPreference) {
    User user = getUserOrCreate(telegramId);
    user.setMinAgePreference(minAge);
    user.setMaxAgePreference(maxAge);
    user.setGenderPreference(genderPreference);
    userRepository.save(user);
    System.out.println("DEBUG: Обновлены настройки поиска для пользователя " + telegramId + 
                      ": возраст " + minAge + "-" + maxAge + ", пол " + genderPreference);
  }
  
  /**
   * Проверяет и обновляет статус заполненности профиля
   */
  private void updateProfileCompleteness(User user) {
    try {
      boolean isComplete = 
          user.getFirstName() != null && !user.getFirstName().isEmpty() &&
          user.getAge() != null &&
          user.getGender() != null && !user.getGender().isEmpty() &&
          user.getDescription() != null && !user.getDescription().isEmpty() &&
          user.getInterests() != null && !user.getInterests().isEmpty() &&
          user.getPhotoFileId() != null && !user.getPhotoFileId().isEmpty();
      
      user.setProfileCompleted(isComplete);
    } catch (Exception e) {
      // Обработка потенциальных проблем с profileCompleted во время миграции
      user.setProfileCompleted(false);
      System.err.println("Ошибка обновления полноты профиля: " + e.getMessage());
    }
  }
  
  /**
   * Возвращает процент заполненности профиля
   */
  public int getProfileCompletionPercentage(Long telegramId) {
    User user = getUserOrCreate(telegramId);
    return user.getProfileCompletionPercentage();
  }
  
  /**
   * Получает пользователя по telegramId или создает нового, если не найден
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
      // Убедимся, что profileCompleted инициализирован во время перехода
      user.setProfileCompleted(false);
      userRepository.save(user);
    }
    return user;
  }
  
  /**
   * Получает форматированную информацию о профиле в виде строки
   */
  public String getUserProfileInfo(Long telegramId) {
    User user = getUserOrCreate(telegramId);
    return user.getProfileInfo();
  }
}