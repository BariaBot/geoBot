package ru.gang.datingBot.service;

import lombok.Getter;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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

  public void updateUserLocation(Long telegramId, double lat, double lon, int hours, int radius) {
    User user = userRepository.findByTelegramId(telegramId).orElse(null);

    if (user == null) {
      user = new User();
      user.setTelegramId(telegramId);
      user.setActive(true);
      user.setUsername("Пользователь " + telegramId); // Заглушка для пустых имен
    }

    user.setLatitude(lat);
    user.setLongitude(lon);
    user.setSearchRadius(radius);
    user.setLastActive(LocalDateTime.now());
    user.setDeactivateAt(LocalDateTime.now().plusHours(hours));

    userRepository.save(user);

    System.out.println("Обновлено местоположение для: " + telegramId +
        " (lat: " + lat + ", lon: " + lon + ", radius: " + radius + ")");
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

  public void deactivateUser(Long telegramId) {
    User user = userRepository.findByTelegramId(telegramId)
        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    user.setActive(false);
    userRepository.save(user);
  }
}
