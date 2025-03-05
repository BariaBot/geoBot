package ru.gang.datingBot.bot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Класс для отправки сообщений и управления сообщениями в Telegram
 */
public class MessageSender {

  private final DatingBot bot;

  public MessageSender(DatingBot bot) {
    this.bot = bot;
  }

  /**
   * Отправляет простое текстовое сообщение
   */
  public void sendTextMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    executeMessage(message);
  }

  /**
   * Отправляет текстовое сообщение с клавиатурой
   */
  public void sendTextMessageWithKeyboard(Long chatId, String text, ReplyKeyboard keyboard) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    message.setReplyMarkup(keyboard);
    executeMessage(message);
  }

  /**
   * Отправляет текстовое сообщение с поддержкой Markdown
   */
  public void sendMarkdownMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(escapeMarkdown(text));
    message.setParseMode("Markdown");
    executeMessage(message);
  }

  /**
   * Отправляет текстовое сообщение с Markdown и клавиатурой
   */
  public void sendMarkdownMessageWithKeyboard(Long chatId, String text, ReplyKeyboard keyboard) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(escapeMarkdown(text));
    message.setParseMode("Markdown");
    message.setReplyMarkup(keyboard);
    executeMessage(message);
  }

  /**
   * Отправляет фотографию с подписью
   */
  public void sendPhoto(Long chatId, String photoFileId, String caption) {
    SendPhoto photoMessage = new SendPhoto();
    photoMessage.setChatId(chatId.toString());
    photoMessage.setPhoto(new InputFile(photoFileId));
    
    if (caption != null && !caption.isEmpty()) {
      photoMessage.setCaption(caption);
    }
    
    try {
      bot.execute(photoMessage);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке фото: " + e.getMessage());
      e.printStackTrace();
      // Запасной вариант - отправляем только текст, если фото не загружается
      if (caption != null && !caption.isEmpty()) {
        sendTextMessage(chatId, caption);
      }
    }
  }

  /**
   * Отправляет фотографию с подписью в формате Markdown
   */
  public void sendPhotoWithMarkdown(Long chatId, String photoFileId, String caption) {
    SendPhoto photoMessage = new SendPhoto();
    photoMessage.setChatId(chatId.toString());
    photoMessage.setPhoto(new InputFile(photoFileId));
    
    if (caption != null && !caption.isEmpty()) {
      photoMessage.setCaption(escapeMarkdown(caption));
      photoMessage.setParseMode("Markdown");
    }
    
    try {
      bot.execute(photoMessage);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке фото с Markdown: " + e.getMessage());
      e.printStackTrace();
      // Запасной вариант - отправляем только текст, если фото не загружается
      if (caption != null && !caption.isEmpty()) {
        sendMarkdownMessage(chatId, caption);
      }
    }
  }

  /**
   * Удаляет сообщение по его ID
   */
  public void deleteMessage(Long chatId, Integer messageId) {
    try {
      bot.execute(new DeleteMessage(chatId.toString(), messageId));
    } catch (TelegramApiException e) {
      // Игнорируем ошибку, если сообщение не найдено
      if (!e.getMessage().contains("message to delete not found")) {
        System.out.println("DEBUG: Ошибка при удалении сообщения: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  /**
   * Выполняет отправку сообщения
   */
  private void executeMessage(SendMessage message) {
    try {
      bot.execute(message);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке сообщения: " + e.getMessage());
      e.printStackTrace();
      
      // Попробуем отправить без разметки, если ошибка связана с форматированием
      if (e.getMessage().contains("can't parse entities")) {
        try {
          // Создаем новое сообщение без разметки
          SendMessage plainMessage = new SendMessage();
          plainMessage.setChatId(message.getChatId());
          plainMessage.setText(message.getText());
          plainMessage.setReplyMarkup(message.getReplyMarkup());
          // Сбрасываем режим разбора
          plainMessage.setParseMode(null);
          
          bot.execute(plainMessage);
          System.out.println("DEBUG: Сообщение отправлено без разметки");
        } catch (TelegramApiException e2) {
          System.out.println("DEBUG: Не удалось отправить даже без разметки: " + e2.getMessage());
        }
      }
    }
  }
  
  /**
   * Экранирует символы для Markdown
   */
  private String escapeMarkdown(String text) {
    if (text == null) {
      return "";
    }
    return text
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("`", "\\`")
            .replace(".", "\\.");
  }
}