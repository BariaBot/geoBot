package ru.gang.datingBot.service;

import lombok.RequiredArgsConstructor;
import ru.gang.datingBot.model.User;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ProfileService {

  private final UserService userService;
  private final KeyboardService keyboardService;

  public String getDisplayName(User user) {
    if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
      return user.getFirstName();
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

    StringBuilder profileInfo = new StringBuilder();
    
    // Отображаем статус VIP, если пользователь имеет активную подписку
    if (profile.isVipActive()) {
      profileInfo.append("👑 ");
    }
    
    profileInfo.append("✨ ").append(displayName);
    
    // Для VIP пользователей показываем username, если он есть
    if (profile.isVipActive() && profile.getUsername() != null && !profile.getUsername().isEmpty()) {
      profileInfo.append(" (@").append(profile.getUsername()).append(")");
    }
    
    profileInfo.append(" рядом!");

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
    
    StringBuilder requestInfo = new StringBuilder();
    
    // Отображаем статус VIP для запросов на встречу
    if (sender.isVipActive()) {
      requestInfo.append("👑 ");
    }
    
    requestInfo.append("✨ ").append(senderName);
    
    // Для VIP пользователей показываем username в запросе
    if (sender.isVipActive() && sender.getUsername() != null && !sender.getUsername().isEmpty()) {
      requestInfo.append(" (@").append(sender.getUsername()).append(")");
    }
    
    requestInfo.append(" отправил вам запрос на встречу!");

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
  
  public String formatVipInfo(User user, boolean isActive, LocalDateTime expiresAt, String planType) {
    StringBuilder vipInfo = new StringBuilder();
    
    vipInfo.append("👑 *VIP-статус*\n\n");
    
    if (isActive) {
      vipInfo.append("✅ У вас активирован VIP-статус!\n");
      vipInfo.append("📅 Действует до: ").append(expiresAt.toLocalDate()).append("\n");
      vipInfo.append("🔄 Текущий тарифный план: ").append(getReadablePlanType(planType)).append("\n\n");
      vipInfo.append("📱 Ваш профиль будет отображаться с пометкой VIP\n");
      vipInfo.append("👁 Пользователи будут видеть ваш username для быстрой связи\n");
      vipInfo.append("🔍 Вы будете отображаться выше в результатах поиска\n");
    } else {
      vipInfo.append("⭐ Получите больше от нашего сервиса с VIP-статусом!\n\n");
      vipInfo.append("📱 Ваш профиль будет отображаться с пометкой VIP\n");
      vipInfo.append("👁 Пользователи будут видеть ваш username для быстрой связи\n");
      vipInfo.append("🔍 Вы будете отображаться выше в результатах поиска\n\n");
      vipInfo.append("Выберите тарифный план ниже для приобретения VIP-статуса:");
    }
    
    return vipInfo.toString();
  }
  
  public String getReadablePlanType(String planType) {
    return switch (planType) {
      case "week" -> "1 неделя";
      case "2weeks" -> "2 недели";
      case "month" -> "1 месяц";
      default -> planType;
    };
  }
  
  public String getSenderDisplayName(User user) {
    StringBuilder name = new StringBuilder();
    
    if (user.isVipActive()) {
      name.append("👑 ");
    }
    
    if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
      name.append(user.getFirstName());
      
      if (user.isVipActive() && user.getUsername() != null && !user.getUsername().isEmpty()) {
        name.append(" (@").append(user.getUsername()).append(")");
      }
      
      return name.toString();
    } else if (user.getUsername() != null && !user.getUsername().isEmpty()) {
      return name.append("@").append(user.getUsername()).toString();
    } else {
      return name.append("Пользователь").toString();
    }
  }
}