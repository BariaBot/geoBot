package ru.gang.datingBot.service;

import lombok.Getter;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Getter
@RequiredArgsConstructor
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

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

    user.setActive(true);

    if (telegramUsername != null && !telegramUsername.isEmpty()) {
      user.setUsername(telegramUsername);
    }

    if (firstName != null && !firstName.isEmpty()) {
      user.setFirstName(firstName);
    }
    if (lastName != null && !lastName.isEmpty()) {
      user.setLastName(lastName);
    }

    if (phoneNumber != null && !phoneNumber.isEmpty()) {
      user.setPhoneNumber(phoneNumber);
    }

    user.setLatitude(lat);
    user.setLongitude(lon);
    user.setSearchRadius(radius);
    user.setLastActive(LocalDateTime.now());
    user.setDeactivateAt(LocalDateTime.now().plusHours(hours));

    if (user.getProfileCompleted() == null) {
      user.setProfileCompleted(false);
    }

    userRepository.save(user);

    log.info("✅ Обновлено местоположение для: {} (lat: {}, lon: {}, username: {}, firstName: {}, lastName: {}, phone: {}, active: {})",
        telegramId,
        lat,
        lon,
        (user.getUsername() != null ? user.getUsername() : "не установлен"),
        user.getFirstName(),
        user.getLastName(),
        user.getPhoneNumber(),
        user.getActive());
  }

  @Scheduled(fixedRate = 600000)
  public void deactivateExpiredUsers() {
    log.info("Проверка активности пользователей...");
    List<User> expiredUsers = userRepository.findExpiredUsers(LocalDateTime.now());
    log.info("Найдено {} пользователей с истекшим временем активности", expiredUsers.size());

    for (User user : expiredUsers) {
      // Деактивируем пользователя, если срок его активности истёк
      user.setActive(false);
      user.setDeactivateAt(LocalDateTime.now());
      userRepository.save(user);

      log.info("Пользователь {} деактивирован", user.getTelegramId());
    }
  }

  public List<User> findNearbyUsers(Long currentUserId, double lat, double lon, int radius) {
    User currentUser = getUserByTelegramId(currentUserId);
    Integer minAge = null;
    Integer maxAge = null;
    String gender = null;

    if (currentUser != null) {
      minAge = currentUser.getMinAgePreference();
      maxAge = currentUser.getMaxAgePreference();

      if (currentUser.getGenderPreference() != null && !currentUser.getGenderPreference().equals("any")) {
        gender = currentUser.getGenderPreference();
      }
    }

    // Отладочная информация
    log.debug("Поиск пользователей с параметрами:");
    log.debug("currentUserId={}", currentUserId);
    log.debug("lat={}, lon={}, radius={}", lat, lon, radius);
    log.debug("minAge={}, maxAge={}, gender={}", minAge, maxAge, gender);

    List<User> users = userRepository.findUsersNearbyWithFilters(lat, lon, radius, currentUserId, minAge, maxAge, gender);

    log.debug("Найдено пользователей: {}", (users != null ? users.size() : 0));
    if (users != null && !users.isEmpty()) {
      for (User user : users) {
        log.debug(" - {} | {} | {} | lat: {} lon: {} | active: {} | vip: {}",
            user.getTelegramId(),
            (user.getUsername() != null ? user.getUsername() : "без username"),
            (user.getFirstName() != null ? user.getFirstName() : ""),
            user.getLatitude(),
            user.getLongitude(),
            user.getActive(),
            (user.getIsVip() != null && user.getIsVip() ? "да" : "нет"));
      }
    } else {
      log.debug("SQL запрос не вернул ни одного пользователя. Пробуем без фильтров.");
      // Если не найдено, пробуем искать без фильтров по возрасту и полу
      users = userRepository.findUsersNearbyWithFilters(lat, lon, radius, currentUserId, null, null, null);
      log.debug("Без фильтров найдено пользователей: {}", (users != null ? users.size() : 0));
    }

    // Сортировка результатов: VIP пользователи в начале списка
    if (users != null && !users.isEmpty()) {
        users = sortUsersByVipStatus(users);
    }

    return users;
  }

  // Сортировка пользователей с приоритетом VIP
  private List<User> sortUsersByVipStatus(List<User> users) {
    LocalDateTime now = LocalDateTime.now();
    
    return users.stream()
        .sorted(Comparator
            // Сначала VIP-пользователи
            .comparing((User u) -> u.getIsVip() != null && u.getIsVip() && 
                                  u.getVipExpiresAt() != null && 
                                  u.getVipExpiresAt().isAfter(now) ? 0 : 1)
            // Затем по последней активности
            .thenComparing((User u) -> u.getLastActive(), Comparator.nullsLast(Comparator.reverseOrder()))
            // И, наконец, по ID
            .thenComparing(User::getId))
        .collect(Collectors.toList());
  }

  public List<User> filterUsersByCommonInterests(List<User> users, Long currentUserId) {
    User currentUser = getUserByTelegramId(currentUserId);
    if (currentUser.getInterests() == null || currentUser.getInterests().isEmpty()) {
      return users;
    }
    
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
    log.debug("Пользователь {} деактивирован вручную", telegramId);
  }
  
  @Transactional
  public void updateUserDescription(Long telegramId, String description) {
    User user = getUserOrCreate(telegramId);
    user.setDescription(description);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  @Transactional
  public void updateUserInterests(Long telegramId, String interests) {
    User user = getUserOrCreate(telegramId);
    user.setInterests(interests);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  @Transactional
  public void updateUserPhoto(Long telegramId, String photoFileId) {
    User user = getUserOrCreate(telegramId);
    user.setPhotoFileId(photoFileId);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  @Transactional
  public void updateUserAge(Long telegramId, Integer age) {
    User user = getUserOrCreate(telegramId);
    user.setAge(age);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  @Transactional
  public void updateUserGender(Long telegramId, String gender) {
    User user = getUserOrCreate(telegramId);
    user.setGender(gender);
    updateProfileCompleteness(user);
    userRepository.save(user);
  }
  
  @Transactional
  public void updateUserSearchPreferences(Long telegramId, Integer minAge, Integer maxAge, String genderPreference) {
    User user = getUserOrCreate(telegramId);
    user.setMinAgePreference(minAge);
    user.setMaxAgePreference(maxAge);
    user.setGenderPreference(genderPreference);
    userRepository.save(user);
    log.debug("Обновлены настройки поиска для пользователя {}: возраст {}-{}, пол {}",
            telegramId, minAge, maxAge, genderPreference);
  }
  
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
      user.setProfileCompleted(false);
      log.error("Ошибка обновления полноты профиля: {}", e.getMessage(), e);
    }
  }
  
  public int getProfileCompletionPercentage(Long telegramId) {
    User user = getUserOrCreate(telegramId);
    return user.getProfileCompletionPercentage();
  }
  
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
      user.setProfileCompleted(false);
      userRepository.save(user);
    }
    return user;
  }
  
  public String getUserProfileInfo(Long telegramId) {
    User user = getUserOrCreate(telegramId);
    return user.getProfileInfo();
  }
}