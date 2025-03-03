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

  // Обновить местоположение пользователя (теперь с username, именем, фамилией и телефоном)
  @PostMapping("/{telegramId}/location")
  public ResponseEntity<String> updateLocation(
      @PathVariable Long telegramId,
      @RequestParam double lat,
      @RequestParam double lon,
      @RequestParam(defaultValue = "1") int hours,
      @RequestParam(defaultValue = "5") int radius,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) String phoneNumber) {

    userService.updateUserLocation(telegramId, lat, lon, hours, radius, username, firstName, lastName, phoneNumber);
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

    // Используем сервис для поиска ближайших пользователей
    List<User> nearbyUsers = userService.findNearbyUsers(
        telegramId, 
        user.getLatitude(), 
        user.getLongitude(), 
        user.getSearchRadius());
    
    return ResponseEntity.ok(nearbyUsers);
  }
  
  // Обновить настройки поиска пользователя
  @PostMapping("/{telegramId}/search-preferences")
  public ResponseEntity<String> updateSearchPreferences(
      @PathVariable Long telegramId,
      @RequestParam(required = false) Integer minAge,
      @RequestParam(required = false) Integer maxAge,
      @RequestParam(required = false) String genderPreference) {
    
    userService.updateUserSearchPreferences(telegramId, minAge, maxAge, genderPreference);
    return ResponseEntity.ok("Настройки поиска обновлены");
  }
  
  // Обновить возраст пользователя
  @PostMapping("/{telegramId}/age")
  public ResponseEntity<String> updateAge(
      @PathVariable Long telegramId,
      @RequestParam Integer age) {
    
    userService.updateUserAge(telegramId, age);
    return ResponseEntity.ok("Возраст обновлен");
  }
  
  // Обновить пол пользователя
  @PostMapping("/{telegramId}/gender")
  public ResponseEntity<String> updateGender(
      @PathVariable Long telegramId,
      @RequestParam String gender) {
    
    userService.updateUserGender(telegramId, gender);
    return ResponseEntity.ok("Пол обновлен");
  }
  
  // Получить процент заполненности профиля
  @GetMapping("/{telegramId}/completion")
  public ResponseEntity<Integer> getProfileCompletion(@PathVariable Long telegramId) {
    int completion = userService.getProfileCompletionPercentage(telegramId);
    return ResponseEntity.ok(completion);
  }
}
