package ru.gang.datingBot.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Сервис для создания различных типов клавиатур в Telegram
 */
public class KeyboardService {

  /**
   * Создает кнопку для инлайн-клавиатуры
   */
  public InlineKeyboardButton createButton(String text, String callbackData) {
    InlineKeyboardButton button = new InlineKeyboardButton();
    button.setText(text);
    button.setCallbackData(callbackData);
    return button;
  }

  /**
   * Создает меню выбора времени для геолокации
   */
  public InlineKeyboardMarkup createTimeSelectionKeyboard() {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    List<InlineKeyboardButton> rowInline = new ArrayList<>();

    rowInline.add(createButton("1 час", "1 час"));
    rowInline.add(createButton("3 часа", "3 часа"));
    rowInline.add(createButton("6 часов", "6 часов"));

    rowsInline.add(rowInline);
    markupInline.setKeyboard(rowsInline);

    return markupInline;
  }

  /**
   * Создает меню выбора радиуса поиска
   */
  public InlineKeyboardMarkup createRadiusSelectionKeyboard() {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    List<InlineKeyboardButton> rowInline = new ArrayList<>();

    rowInline.add(createButton("1 км", "1 км"));
    rowInline.add(createButton("3 км", "3 км"));
    rowInline.add(createButton("5 км", "5 км"));
    rowInline.add(createButton("1500 км", "1500 км"));

    rowsInline.add(rowInline);
    markupInline.setKeyboard(rowsInline);

    return markupInline;
  }

  /**
   * Создает клавиатуру выбора пола
   */
  public InlineKeyboardMarkup createGenderSelectionKeyboard() {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    List<InlineKeyboardButton> rowInline = new ArrayList<>();
    rowInline.add(createButton("Мужской", "gender_male"));
    rowInline.add(createButton("Женский", "gender_female"));
    rowsInline.add(rowInline);

    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("Другой", "gender_other"));
    rowsInline.add(row2);

    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

  /**
   * Создает клавиатуру выбора предпочитаемого пола
   */
  public InlineKeyboardMarkup createGenderPreferenceKeyboard() {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    List<InlineKeyboardButton> rowInline = new ArrayList<>();
    rowInline.add(createButton("Мужской", "gender_pref_male"));
    rowInline.add(createButton("Женский", "gender_pref_female"));
    rowsInline.add(rowInline);

    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("Любой", "gender_pref_any"));
    rowsInline.add(row2);

    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

  /**
   * Создает клавиатуру для редактирования профиля
   */
  public InlineKeyboardMarkup createProfileEditKeyboard() {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    List<InlineKeyboardButton> row1 = new ArrayList<>();
    row1.add(createButton("Описание", "edit_profile_description"));
    row1.add(createButton("Интересы", "edit_profile_interests"));
    rowsInline.add(row1);

    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("Возраст", "edit_profile_age"));
    row2.add(createButton("Пол", "edit_profile_gender"));
    rowsInline.add(row2);

    List<InlineKeyboardButton> row3 = new ArrayList<>();
    row3.add(createButton("Фото", "edit_profile_photo"));
    row3.add(createButton("Настройки поиска", "edit_profile_search"));
    rowsInline.add(row3);

    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

  /**
   * Создает клавиатуру для поиска пользователей поблизости
   */
  public InlineKeyboardMarkup createNearbyUserNavigationKeyboard(Long targetUserId, boolean hasMultipleUsers) {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    // Добавляем кнопки навигации только при наличии нескольких пользователей
    if (hasMultipleUsers) {
      List<InlineKeyboardButton> navigationRow = new ArrayList<>();
      navigationRow.add(createButton("⬅️ Предыдущий", "prev_user"));
      navigationRow.add(createButton("Следующий ➡️", "next_user"));
      rowsInline.add(navigationRow);
    }

    // Кнопка для отправки запроса на встречу
    List<InlineKeyboardButton> actionRow = new ArrayList<>();
    actionRow.add(createButton("📩 Отправить запрос", "send_request_" + targetUserId));
    rowsInline.add(actionRow);

    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

  /**
   * Создает клавиатуру для запросов на встречу
   */
  public InlineKeyboardMarkup createMeetingRequestKeyboard(Long senderId) {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    List<InlineKeyboardButton> rowInline = new ArrayList<>();

    rowInline.add(createButton("✅ Принять", "accept_request_" + senderId));
    rowInline.add(createButton("❌ Отклонить", "decline_request_" + senderId));

    rowsInline.add(rowInline);
    markupInline.setKeyboard(rowsInline);

    return markupInline;
  }

  /**
   * Создает клавиатуру для настроек поиска
   */
  public InlineKeyboardMarkup createSearchSettingsKeyboard() {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    List<InlineKeyboardButton> row1 = new ArrayList<>();
    row1.add(createButton("Возрастной диапазон", "edit_profile_age_range"));
    rowsInline.add(row1);

    List<InlineKeyboardButton> row2 = new ArrayList<>();
    row2.add(createButton("Предпочитаемый пол", "edit_profile_gender_pref"));
    rowsInline.add(row2);

    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

  /**
   * Создает клавиатуру с кнопкой запроса геолокации
   */
  public ReplyKeyboardMarkup createLocationRequestKeyboard() {
    KeyboardButton locationButton = new KeyboardButton("📍 Поделиться геолокацией");
    locationButton.setRequestLocation(true);

    KeyboardButton stopButton = new KeyboardButton("❌ Остановить поиск");

    KeyboardRow row = new KeyboardRow();
    row.add(locationButton);

    KeyboardRow row2 = new KeyboardRow();
    row2.add(stopButton);

    List<KeyboardRow> keyboard = new ArrayList<>();
    keyboard.add(row);
    keyboard.add(row2);

    ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
    replyKeyboardMarkup.setKeyboard(keyboard);
    replyKeyboardMarkup.setResizeKeyboard(true);

    return replyKeyboardMarkup;
  }

  /**
   * Создает основную клавиатуру с кнопками для быстрого доступа
   */
  public ReplyKeyboardMarkup createMainKeyboard() {
    ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
    markup.setResizeKeyboard(true);
    markup.setSelective(false);
    markup.setOneTimeKeyboard(false);

    List<KeyboardRow> keyboard = new ArrayList<>();

    KeyboardRow row1 = new KeyboardRow();
    row1.add(new KeyboardButton("🔄 Обновить геолокацию"));
    row1.add(new KeyboardButton("👤 Мой профиль"));

    KeyboardRow row2 = new KeyboardRow();
    row2.add(new KeyboardButton("❌ Остановить поиск"));

    keyboard.add(row1);
    keyboard.add(row2);

    markup.setKeyboard(keyboard);
    return markup;
  }

  /**
   * Создает клавиатуру для навигации по местам встречи
   */
  public InlineKeyboardMarkup createPlaceNavigationKeyboard(Long placeId, boolean hasMultiplePlaces) {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    
    // Кнопки навигации
    if (hasMultiplePlaces) {
        List<InlineKeyboardButton> navigationRow = new ArrayList<>();
        navigationRow.add(createButton("⬅️ Предыдущее", "prev_place"));
        navigationRow.add(createButton("Следующее ➡️", "next_place"));
        rowsInline.add(navigationRow);
    }
    
    // Кнопка выбора места
    List<InlineKeyboardButton> actionRow = new ArrayList<>();
    actionRow.add(createButton("✅ Выбрать это место", "select_place_" + placeId));
    rowsInline.add(actionRow);
    
    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

  /**
   * Создает клавиатуру для выбора даты встречи
   */
  public InlineKeyboardMarkup createDateSelectionKeyboard() {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    
    LocalDate today = LocalDate.now();
    
    // Создаем кнопки для 7 дней
    for (int i = 0; i < 7; i += 2) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        
        LocalDate date1 = today.plusDays(i);
        row.add(createButton(
            formatDate(date1), 
            "date_" + date1.format(DateTimeFormatter.ISO_DATE)
        ));
        
        if (i + 1 < 7) {
            LocalDate date2 = today.plusDays(i + 1);
            row.add(createButton(
                formatDate(date2), 
                "date_" + date2.format(DateTimeFormatter.ISO_DATE)
            ));
        }
        
        rowsInline.add(row);
    }
    
    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

  /**
   * Форматирует дату для отображения
   */
  private String formatDate(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM (E)", new Locale("ru"));
    return date.format(formatter);
  }

  /**
   * Создает клавиатуру для выбора времени встречи
   */
  public InlineKeyboardMarkup createMeetingTimeSelectionKeyboard() {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    
    // Временные слоты с 10:00 до 21:00
    for (int hour = 10; hour <= 20; hour += 2) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        
        String time1 = String.format("%02d:00", hour);
        row.add(createButton(time1, "time_" + time1));
        
        if (hour + 1 <= 20) {
            String time2 = String.format("%02d:00", hour + 1);
            row.add(createButton(time2, "time_" + time2));
        }
        
        rowsInline.add(row);
    }
    
    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

  /**
   * Создает клавиатуру для подтверждения встречи
   */
  public InlineKeyboardMarkup createConfirmMeetingKeyboard(Long meetingRequestId) {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    
    List<InlineKeyboardButton> row = new ArrayList<>();
    row.add(createButton("✅ Подтвердить", "confirm_meeting_" + meetingRequestId));
    rowsInline.add(row);
    
    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

  /**
   * Создает клавиатуру для сбора обратной связи
   */
  public InlineKeyboardMarkup createFeedbackKeyboard(Long meetingRequestId) {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
    
    // Оценка от 1 до 5
    List<InlineKeyboardButton> ratingRow = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
        ratingRow.add(createButton(i + "⭐", "rate_meeting_" + meetingRequestId + "_" + i));
    }
    rowsInline.add(ratingRow);
    
    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }
}
