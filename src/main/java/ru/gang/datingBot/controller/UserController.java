package ru.gang.datingBot.controller;

import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  // Получить список всех пользователей
  @GetMapping
  public ResponseEntity<List<User>> getAllUsers() {
    return ResponseEntity.ok(userService.getAllUsers());
  }

  // Получить пользователя по Telegram ID
  @GetMapping("/{telegramId}")
  public ResponseEntity<User> getUserByTelegramId(@PathVariable Long telegramId) {
    User user = userService.getUserByTelegramId(telegramId);
    return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
  }

  // Обновить местоположение пользователя
  @PostMapping("/{telegramId}/location")
  public ResponseEntity<String> updateLocation(@PathVariable Long telegramId, @RequestParam double lat, @RequestParam double lon) {
    userService.updateUserLocation(telegramId, lat, lon, 1, 5);
    return ResponseEntity.ok("Местоположение обновлено");
  }

  // Деактивировать пользователя
  @PostMapping("/{telegramId}/deactivate")
  public ResponseEntity<String> deactivateUser(@PathVariable Long telegramId) {
    userService.deactivateUser(telegramId);
    return ResponseEntity.ok("Пользователь отключен от поиска");
  }

  // Получить список пользователей рядом
  @GetMapping("/{telegramId}/nearby")
  public ResponseEntity<List<User>> getNearbyUsers(@PathVariable Long telegramId) {
    User user = userService.getUserByTelegramId(telegramId);
    if (user == null) return ResponseEntity.notFound().build();

    List<User> nearbyUsers = userService.findNearbyUsers(user.getLatitude(), user.getLongitude(), user.getSearchRadius());
    return ResponseEntity.ok(nearbyUsers);
  }
}
