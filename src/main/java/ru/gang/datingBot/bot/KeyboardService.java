package ru.gang.datingBot.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardService {

  public InlineKeyboardButton createButton(String text, String callbackData) {
    InlineKeyboardButton button = new InlineKeyboardButton();
    button.setText(text);
    button.setCallbackData(callbackData);
    return button;
  }

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

  public InlineKeyboardMarkup createNearbyUserNavigationKeyboard(Long targetUserId, boolean hasMultipleUsers) {
    InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    if (hasMultipleUsers) {
      List<InlineKeyboardButton> navigationRow = new ArrayList<>();
      navigationRow.add(createButton("⬅️ Предыдущий", "prev_user"));
      navigationRow.add(createButton("Следующий ➡️", "next_user"));
      rowsInline.add(navigationRow);
    }

    List<InlineKeyboardButton> actionRow = new ArrayList<>();
    actionRow.add(createButton("📩 Отправить запрос", "send_request_" + targetUserId));
    rowsInline.add(actionRow);

    markupInline.setKeyboard(rowsInline);
    return markupInline;
  }

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
}