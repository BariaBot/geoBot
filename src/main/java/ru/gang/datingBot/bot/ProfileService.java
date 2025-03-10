package ru.gang.datingBot.bot;

import lombok.RequiredArgsConstructor;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.UserService;

@RequiredArgsConstructor
public class ProfileService {

  private final UserService userService;
  private final KeyboardService keyboardService;

  public String getDisplayName(User user) {
    if (user.getUsername() != null && !user.getUsername().isEmpty()) {
      return user.getUsername();
    } else if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
      String displayName = user.getFirstName();
      if (user.getLastName() != null && !user.getLastName().isEmpty()) {
        displayName += " " + user.getLastName();
      }
      return displayName;
    } else {
      return "Анонимный пользователь";
    }
  }

  public String getGenderDisplay(String gender) {
    if (gender == null) return "Не указан";
    return switch (gender) {
      case "male" -> "Мужской";
      case "female" -> "Женский";
      case "other" -> "Другой";
      default -> "Не указан";
    };
  }

  public String getGenderPreferenceDisplay(String genderPref) {
    if (genderPref == null) return "Любой";
    return switch (genderPref) {
      case "male" -> "Мужской";
      case "female" -> "Женский";
      case "any" -> "Любой";
      default -> "Любой";
    };
  }

  public String formatNearbyUserProfile(User profile, int currentIndex, int totalUsers) {
    String displayName = getDisplayName(profile);
    String username = profile.getUsername() != null ? "@" + profile.getUsername() : "Нет username";

    StringBuilder profileInfo = new StringBuilder();
    profileInfo.append("✨ ").append(displayName).append(" рядом!");
    profileInfo.append("\n📱 Username: ").append(username);

    if (profile.getAge() != null) {
      profileInfo.append("\n\n🎂 Возраст: ").append(profile.getAge());
    }

    if (profile.getGender() != null && !profile.getGender().isEmpty()) {
      profileInfo.append("\n⚧ Пол: ").append(profile.getGenderDisplay());
    }

    if (profile.getDescription() != null && !profile.getDescription().isEmpty()) {
      profileInfo.append("\n\n📝 О себе: ").append(profile.getDescription());
    }

    if (profile.getInterests() != null && !profile.getInterests().isEmpty()) {
      profileInfo.append("\n\n⭐ Интересы: ").append(profile.getInterests());
    }

    profileInfo.append("\n\n🔢 Профиль ").append(currentIndex + 1).append(" из ").append(totalUsers);

    return profileInfo.toString();
  }

  public String formatMeetingRequest(User sender, String message) {
    String senderName = getDisplayName(sender);
    String username = sender.getUsername() != null ? "@" + sender.getUsername() : "Нет username";
    
    StringBuilder requestInfo = new StringBuilder();
    requestInfo.append("✨ ").append(senderName).append(" отправил вам запрос на встречу!");
    requestInfo.append("\n📱 Username: ").append(username);

    if (sender.getAge() != null) {
      requestInfo.append("\n\n🎂 Возраст: ").append(sender.getAge());
    }

    if (sender.getGender() != null && !sender.getGender().isEmpty()) {
      requestInfo.append("\n⚧ Пол: ").append(sender.getGenderDisplay());
    }

    if (sender.getDescription() != null && !sender.getDescription().isEmpty()) {
      requestInfo.append("\n\n📝 О себе: ").append(sender.getDescription());
    }

    if (sender.getInterests() != null && !sender.getInterests().isEmpty()) {
      requestInfo.append("\n\n⭐ Интересы: ").append(sender.getInterests());
    }

    requestInfo.append("\n\n💬 Сообщение: ").append(message);

    return requestInfo.toString();
  }

  public String formatSearchSettings(User user) {
    StringBuilder settingsInfo = new StringBuilder();
    settingsInfo.append("🔍 *Ваши настройки поиска:*\n\n");
    
    String ageRange = "Любой";
    if (user.getMinAgePreference() != null && user.getMaxAgePreference() != null) {
      ageRange = user.getMinAgePreference() + " - " + user.getMaxAgePreference() + " лет";
    } else if (user.getMinAgePreference() != null) {
      ageRange = "от " + user.getMinAgePreference() + " лет";
    } else if (user.getMaxAgePreference() != null) {
      ageRange = "до " + user.getMaxAgePreference() + " лет";
    }
    
    settingsInfo.append("🎯 *Возраст:* ").append(ageRange).append("\n");
    settingsInfo.append("👥 *Пол:* ").append(user.getGenderPreferenceDisplay()).append("\n");
    settingsInfo.append("📍 *Радиус поиска:* ").append(user.getSearchRadius()).append(" км\n");
    
    return settingsInfo.toString();
  }
}