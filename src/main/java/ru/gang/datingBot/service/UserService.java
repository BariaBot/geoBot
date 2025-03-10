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

@Service
@Getter
@RequiredArgsConstructor
public class UserService {

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

    System.out.println("✅ Обновлено местоположение для: " + telegramId +
        " (lat: " + lat + ", lon: " + lon + 
        ", username: " + (user.getUsername() != null ? user.getUsername() : "не установлен") +
        ", firstName: " + user.getFirstName() + 
        ", lastName: " + user.getLastName() + 
        ", phone: " + user.getPhoneNumber() + 
        ", active: " + user.getActive() + ")");
  }

  @Scheduled(fixedRate = 600000)
  public void deactivateExpiredUsers() {
    System.out.println("Проверка активности пользователей...");
    List<User> expiredUsers = userRepository.findExpiredUsers(LocalDateTime.now());
    System.out.println("Найдено " + expiredUsers.size() + " пользователей с истекшим временем активности");
    for (User user : expiredUsers) {
      user.setDeactivateAt(LocalDateTime.now().plusDays(30));
      userRepository.save(user);
      System.out.println("Продлена активность пользователя " + user.getTelegramId() + " на 30 дней");
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
    System.out.println("DEBUG: Поиск пользователей с параметрами:");
    System.out.println("DEBUG: currentUserId=" + currentUserId);
    System.out.println("DEBUG: lat=" + lat + ", lon=" + lon + ", radius=" + radius);
    System.out.println("DEBUG: minAge=" + minAge + ", maxAge=" + maxAge + ", gender=" + gender);

    List<User> users = userRepository.findUsersNearbyWithFilters(lat, lon, radius, currentUserId, minAge, maxAge, gender);

    System.out.println("Найдено пользователей: " + (users != null ? users.size() : 0));
    if (users != null && !users.isEmpty()) {
      for (User user : users) {
        System.out.println(" - " + user.getTelegramId() + " | " +
            (user.getUsername() != null ? user.getUsername() : "без username") +
            " | " + (user.getFirstName() != null ? user.getFirstName() : "") +
            " | lat: " + user.getLatitude() + " lon: " + user.getLongitude() +
            " | active: " + user.getActive() +
            " | vip: " + (user.getIsVip() != null && user.getIsVip() ? "да" : "нет"));
      }
    } else {
      System.out.println("DEBUG: SQL запрос не вернул ни одного пользователя. Пробуем без фильтров.");
      // Если не найдено, пробуем искать без фильтров по возрасту и полу
      users = userRepository.findUsersNearbyWithFilters(lat, lon, radius, currentUserId, null, null, null);
      System.out.println("DEBUG: Без фильтров найдено пользователей: " + (users != null ? users.size() : 0));
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
    System.out.println("DEBUG: Пользователь " + telegramId + " деактивирован вручную");
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
    System.out.println("DEBUG: Обновлены настройки поиска для пользователя " + telegramId + 
                      ": возраст " + minAge + "-" + maxAge + ", пол " + genderPreference);
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
      System.err.println("Ошибка обновления полноты профиля: " + e.getMessage());
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