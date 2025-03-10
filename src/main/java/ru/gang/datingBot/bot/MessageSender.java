package ru.gang.datingBot.bot;

import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RequiredArgsConstructor
public class MessageSender {

  private final DatingBot bot;

  public void sendTextMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    executeMessage(message);
  }

  public void sendTextMessageWithKeyboard(Long chatId, String text, ReplyKeyboard keyboard) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    message.setReplyMarkup(keyboard);
    executeMessage(message);
  }

  public void sendMarkdownMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(escapeMarkdown(text));
    message.setParseMode("Markdown");
    executeMessage(message);
  }

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
      if (caption != null && !caption.isEmpty()) {
        sendTextMessage(chatId, caption);
      }
    }
  }

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
      if (caption != null && !caption.isEmpty()) {
        sendMarkdownMessage(chatId, caption);
      }
    }
  }

  public void sendSticker(Long chatId, String stickerFileId) {
    SendSticker stickerMessage = new SendSticker();
    stickerMessage.setChatId(chatId.toString());
    stickerMessage.setSticker(new InputFile(stickerFileId));
    
    try {
      bot.execute(stickerMessage);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке стикера: " + e.getMessage());
      e.printStackTrace();
      sendTextMessage(chatId, "⚠️ Не удалось отправить стикер");
    }
  }

  public void sendAnimation(Long chatId, String animationFileId, String caption) {
    SendAnimation animationMessage = new SendAnimation();
    animationMessage.setChatId(chatId.toString());
    animationMessage.setAnimation(new InputFile(animationFileId));
    
    if (caption != null && !caption.isEmpty()) {
      animationMessage.setCaption(caption);
    }
    
    try {
      bot.execute(animationMessage);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке анимации: " + e.getMessage());
      e.printStackTrace();
      if (caption != null && !caption.isEmpty()) {
        sendTextMessage(chatId, caption);
      }
    }
  }

  public void sendVideo(Long chatId, String videoFileId, String caption) {
    SendVideo videoMessage = new SendVideo();
    videoMessage.setChatId(chatId.toString());
    videoMessage.setVideo(new InputFile(videoFileId));
    
    if (caption != null && !caption.isEmpty()) {
      videoMessage.setCaption(caption);
    }
    
    try {
      bot.execute(videoMessage);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке видео: " + e.getMessage());
      e.printStackTrace();
      if (caption != null && !caption.isEmpty()) {
        sendTextMessage(chatId, caption);
      }
    }
  }

  public void sendVoice(Long chatId, String voiceFileId, String caption) {
    SendVoice voiceMessage = new SendVoice();
    voiceMessage.setChatId(chatId.toString());
    voiceMessage.setVoice(new InputFile(voiceFileId));
    
    if (caption != null && !caption.isEmpty()) {
      voiceMessage.setCaption(caption);
    }
    
    try {
      bot.execute(voiceMessage);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке голосового сообщения: " + e.getMessage());
      e.printStackTrace();
      if (caption != null && !caption.isEmpty()) {
        sendTextMessage(chatId, caption);
      }
    }
  }

  public void sendAudio(Long chatId, String audioFileId, String caption) {
    SendAudio audioMessage = new SendAudio();
    audioMessage.setChatId(chatId.toString());
    audioMessage.setAudio(new InputFile(audioFileId));
    
    if (caption != null && !caption.isEmpty()) {
      audioMessage.setCaption(caption);
    }
    
    try {
      bot.execute(audioMessage);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке аудио: " + e.getMessage());
      e.printStackTrace();
      if (caption != null && !caption.isEmpty()) {
        sendTextMessage(chatId, caption);
      }
    }
  }

  public void sendDocument(Long chatId, String documentFileId, String caption) {
    SendDocument documentMessage = new SendDocument();
    documentMessage.setChatId(chatId.toString());
    documentMessage.setDocument(new InputFile(documentFileId));
    
    if (caption != null && !caption.isEmpty()) {
      documentMessage.setCaption(caption);
    }
    
    try {
      bot.execute(documentMessage);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке документа: " + e.getMessage());
      e.printStackTrace();
      if (caption != null && !caption.isEmpty()) {
        sendTextMessage(chatId, caption);
      }
    }
  }

  public void deleteMessage(Long chatId, Integer messageId) {
    try {
      bot.execute(new DeleteMessage(chatId.toString(), messageId));
    } catch (TelegramApiException e) {
      if (!e.getMessage().contains("message to delete not found")) {
        System.out.println("DEBUG: Ошибка при удалении сообщения: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private void executeMessage(SendMessage message) {
    try {
      bot.execute(message);
    } catch (TelegramApiException e) {
      System.out.println("DEBUG: Ошибка при отправке сообщения: " + e.getMessage());
      e.printStackTrace();
      
      if (e.getMessage().contains("can't parse entities")) {
        try {
          SendMessage plainMessage = new SendMessage();
          plainMessage.setChatId(message.getChatId());
          plainMessage.setText(message.getText());
          plainMessage.setReplyMarkup(message.getReplyMarkup());
          plainMessage.setParseMode(null);
          
          bot.execute(plainMessage);
          System.out.println("DEBUG: Сообщение отправлено без разметки");
        } catch (TelegramApiException e2) {
          System.out.println("DEBUG: Не удалось отправить даже без разметки: " + e2.getMessage());
        }
      }
    }
  }

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