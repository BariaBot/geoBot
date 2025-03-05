package ru.gang.datingBot.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.gang.datingBot.bot.KeyboardService;
import ru.gang.datingBot.bot.MessageSender;
import ru.gang.datingBot.bot.UserStateManager;
import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.model.Place;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.service.MeetingService;
import ru.gang.datingBot.service.PlaceService;
import ru.gang.datingBot.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class MeetingPlaceHandler {

    private final UserService userService;
    private final MeetingService meetingService;
    private final PlaceService placeService;
    private final UserStateManager stateManager;
    private final KeyboardService keyboardService;
    private final MessageSender messageSender;

    /**
     * Обрабатывает команду /meet
     */
    public void processMeetCommand(Long chatId) {
        Long targetUserId = stateManager.getCurrentChatUser(chatId);
        Long meetingRequestId = stateManager.getCurrentChatMeetingRequest(chatId);
        
        if (targetUserId == null || meetingRequestId == null) {
            messageSender.sendTextMessage(chatId, "⚠️ Чат не активен. Вы должны сначала создать чат с пользователем.");
            return;
        }
        
        // Начинаем процесс выбора места
        messageSender.sendTextMessage(chatId, 
                "🏙️ Давайте выберем место для встречи! Напишите тип заведения (например, кофейня, ресторан, парк):");
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_PLACE_TYPE);
    }

    /**
     * Обрабатывает ввод типа места
     */
    public void processPlaceTypeInput(Long chatId, String placeType) {
        Long targetUserId = stateManager.getCurrentChatUser(chatId);
        Long meetingRequestId = stateManager.getCurrentChatMeetingRequest(chatId);
        
        User currentUser = userService.getUserByTelegramId(chatId);
        User targetUser = userService.getUserByTelegramId(targetUserId);
        
        if (currentUser == null || targetUser == null) {
            messageSender.sendTextMessage(chatId, "⚠️ Произошла ошибка. Пользователь не найден.");
            stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
            return;
        }
        
        messageSender.sendTextMessage(chatId, "🔍 Ищем " + placeType + " поблизости...");
        
        List<Place> places = placeService.findPlacesBetweenUsers(
                currentUser, targetUser, placeType, 1000);
        
        if (places.isEmpty()) {
            messageSender.sendTextMessage(chatId, 
                    "😔 К сожалению, не удалось найти подходящие места поблизости. Попробуйте другой тип заведения.");
            return;
        }
        
        // Сохраняем найденные места в кэше
        stateManager.cachePlaces(chatId, places);
        stateManager.setCurrentPlaceIndex(chatId, 0);
        
        // Запоминаем текущий meetingRequestId
        stateManager.savePendingMeetingRequestId(chatId, meetingRequestId);
        
        // Показываем первое место
        showCurrentPlace(chatId);
        
        // Уведомляем собеседника о начале выбора места
        String senderName = currentUser.getFirstName() != null ? 
                currentUser.getFirstName() : (currentUser.getUsername() != null ? "@" + currentUser.getUsername() : "Пользователь");
        
        messageSender.sendTextMessage(targetUserId, 
                "🏙️ " + senderName + " предлагает выбрать место для встречи: " + placeType);
        
        stateManager.setUserState(chatId, UserStateManager.UserState.VIEWING_PLACES);
    }

    /**
     * Показывает текущее место из списка
     */
    public void showCurrentPlace(Long chatId) {
        List<Place> places = stateManager.getCachedPlaces(chatId);
        Integer currentIndex = stateManager.getCurrentPlaceIndex(chatId);
        
        if (places == null || places.isEmpty() || currentIndex == null) {
            messageSender.sendTextMessage(chatId, "⚠️ Произошла ошибка при отображении мест.");
            return;
        }
        
        Place place = places.get(currentIndex);
        
        StringBuilder info = new StringBuilder();
        info.append("🏢 *").append(place.getName()).append("*\n\n");
        if (place.getAddress() != null) {
            info.append("📍 Адрес: ").append(place.getAddress()).append("\n");
        }
        if (place.getRating() != null) {
            info.append("⭐ Рейтинг: ").append(place.getRating()).append("/5\n\n");
        }
        if (place.getDescription() != null && !place.getDescription().isEmpty()) {
            info.append(place.getDescription()).append("\n\n");
        }
        info.append("🔢 Место ").append(currentIndex + 1).append(" из ").append(places.size());
        
        // Создаем клавиатуру для навигации
        InlineKeyboardMarkup keyboard = keyboardService.createPlaceNavigationKeyboard(
                place.getId(), places.size() > 1);
        
        if (place.getPhotoUrl() != null && !place.getPhotoUrl().isEmpty()) {
            messageSender.sendPhotoWithMarkdown(chatId, place.getPhotoUrl(), info.toString());
            messageSender.sendTextMessageWithKeyboard(chatId, "Выберите действие:", keyboard);
        } else {
            messageSender.sendMarkdownMessageWithKeyboard(chatId, info.toString(), keyboard);
        }
    }

    /**
     * Показывает следующее место
     */
    public void showNextPlace(Long chatId) {
        List<Place> places = stateManager.getCachedPlaces(chatId);
        Integer currentIndex = stateManager.getCurrentPlaceIndex(chatId);
        
        if (places == null || places.isEmpty() || currentIndex == null) {
            messageSender.sendTextMessage(chatId, "⚠️ Список мест не найден. Пожалуйста, начните поиск заново.");
            return;
        }
        
        // Переходим к следующему месту или возвращаемся к началу
        currentIndex = (currentIndex + 1) % places.size();
        stateManager.setCurrentPlaceIndex(chatId, currentIndex);
        
        // Показываем новое место
        showCurrentPlace(chatId);
    }

    /**
     * Показывает предыдущее место
     */
    public void showPreviousPlace(Long chatId) {
        List<Place> places = stateManager.getCachedPlaces(chatId);
        Integer currentIndex = stateManager.getCurrentPlaceIndex(chatId);
        
        if (places == null || places.isEmpty() || currentIndex == null) {
            messageSender.sendTextMessage(chatId, "⚠️ Список мест не найден. Пожалуйста, начните поиск заново.");
            return;
        }
        
        // Переходим к предыдущему месту или к последнему в списке
        currentIndex = (currentIndex - 1 + places.size()) % places.size();
        stateManager.setCurrentPlaceIndex(chatId, currentIndex);
        
        // Показываем новое место
        showCurrentPlace(chatId);
    }

    /**
     * Обрабатывает выбор места
     */
    public void processPlaceSelection(Long chatId, Long placeId) {
        Place place = placeService.getPlaceById(placeId);
        
        if (place == null) {
            messageSender.sendTextMessage(chatId, "⚠️ Выбранное место не найдено. Пожалуйста, попробуйте снова.");
            return;
        }
        
        // Сохраняем выбранное место
        stateManager.saveSelectedPlaceId(chatId, placeId);
        
        // Показываем клавиатуру для выбора даты
        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "📅 Отлично! Вы выбрали " + place.getName() + ". Теперь выберите день встречи:",
                keyboardService.createDateSelectionKeyboard());
        
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_DATE);
    }

    /**
     * Обрабатывает выбор даты
     */
    public void processDateSelection(Long chatId, String dateString) {
        stateManager.saveSelectedDate(chatId, dateString);
        
        // Показываем клавиатуру для выбора времени
        messageSender.sendTextMessageWithKeyboard(
                chatId,
                "🕒 Выберите время встречи:",
                keyboardService.createMeetingTimeSelectionKeyboard());
        
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_TIME);
    }

    /**
     * Обрабатывает выбор времени
     */
    public void processTimeSelection(Long chatId, String timeString) {
        stateManager.saveSelectedTime(chatId, timeString);
        
        // Получаем все выбранные данные
        Long placeId = stateManager.getSelectedPlaceId(chatId);
        String dateString = stateManager.getSelectedDate(chatId);
        Long meetingRequestId = stateManager.getPendingMeetingRequestId(chatId);
        Long targetUserId = stateManager.getCurrentChatUser(chatId);
        
        if (placeId == null || dateString == null || timeString == null || 
            meetingRequestId == null || targetUserId == null) {
            messageSender.sendTextMessage(chatId, "⚠️ Произошла ошибка. Попробуйте начать процесс заново.");
            stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
            stateManager.clearMeetingPlaceData(chatId);
            return;
        }
        
        // Получаем место
        Place place = placeService.getPlaceById(placeId);
        
        // Создаем дату и время встречи
        LocalDate date = LocalDate.parse(dateString);
        LocalTime time = LocalTime.parse(timeString);
        LocalDateTime meetingDateTime = LocalDateTime.of(date, time);
        
        // Обновляем запрос на встречу
        boolean success = meetingService.updateMeetingRequestWithPlace(
                meetingRequestId, place, meetingDateTime, chatId);
        
        if (!success) {
            messageSender.sendTextMessage(chatId, "⚠️ Произошла ошибка при обновлении запроса на встречу.");
            return;
        }
        
        // Форматируем дату и время для отображения
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("ru"));
        
        // Создаем сообщение с информацией о встрече
        StringBuilder meetingInfo = new StringBuilder();
        meetingInfo.append("📝 Подтвердите информацию о встрече:\n\n");
        meetingInfo.append("🏢 Место: ").append(place.getName()).append("\n");
        if (place.getAddress() != null) {
            meetingInfo.append("📍 Адрес: ").append(place.getAddress()).append("\n");
        }
        meetingInfo.append("📅 Дата: ").append(date.format(dateFormatter)).append("\n");
        meetingInfo.append("🕒 Время: ").append(timeString).append("\n\n");
        meetingInfo.append("Пожалуйста, подтвердите встречу, нажав на кнопку ниже.");
        
        // Отправляем информацию о встрече с кнопкой подтверждения
        messageSender.sendTextMessageWithKeyboard(
                chatId,
                meetingInfo.toString(),
                keyboardService.createConfirmMeetingKeyboard(meetingRequestId));
        
        // Отправляем собеседнику информацию о предложенной встрече
        User sender = userService.getUserByTelegramId(chatId);
        String senderName = sender.getFirstName() != null ? 
                sender.getFirstName() : (sender.getUsername() != null ? "@" + sender.getUsername() : "Пользователь");
        
        StringBuilder partnerInfo = new StringBuilder();
        partnerInfo.append("📢 ").append(senderName).append(" предлагает встречу:\n\n");
        partnerInfo.append("🏢 Место: ").append(place.getName()).append("\n");
        if (place.getAddress() != null) {
            partnerInfo.append("📍 Адрес: ").append(place.getAddress()).append("\n");
        }
        partnerInfo.append("📅 Дата: ").append(date.format(dateFormatter)).append("\n");
        partnerInfo.append("🕒 Время: ").append(timeString).append("\n\n");
        partnerInfo.append("Пожалуйста, подтвердите или предложите свой вариант с помощью команды /meet");
        
        messageSender.sendTextMessageWithKeyboard(
                targetUserId,
                partnerInfo.toString(),
                keyboardService.createConfirmMeetingKeyboard(meetingRequestId));
        
        stateManager.setUserState(chatId, UserStateManager.UserState.WAITING_FOR_CONFIRMATION);
    }

    /**
     * Обрабатывает подтверждение встречи
     */
    public void processConfirmation(Long chatId, Long meetingRequestId) {
        boolean confirmed = meetingService.confirmMeetingByUser(meetingRequestId, chatId);
        
        if (confirmed) {
            // Получаем полную информацию о встрече
            MeetingRequest request = meetingService.getMeetingRequestById(meetingRequestId);
            
            if (request != null && request.isPlaceConfirmedByBoth() && request.getSelectedPlace() != null) {
                // Обоими пользователями подтверждена встреча
                // Отправляем уведомление о подтверждении обоим участникам
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", new Locale("ru"));
                String meetingTimeFormatted = request.getMeetingTime().format(dateFormatter);
                Place place = request.getSelectedPlace();
                
                StringBuilder confirmationMessage = new StringBuilder();
                confirmationMessage.append("✅ Встреча подтверждена обоими участниками!\n\n");
                confirmationMessage.append("🏢 Место: ").append(place.getName()).append("\n");
                if (place.getAddress() != null) {
                    confirmationMessage.append("📍 Адрес: ").append(place.getAddress()).append("\n");
                }
                confirmationMessage.append("📅 Дата и время: ").append(meetingTimeFormatted).append("\n\n");
                confirmationMessage.append("Желаем приятной встречи! 🎉");
                
                messageSender.sendTextMessage(request.getSender().getTelegramId(), confirmationMessage.toString());
                messageSender.sendTextMessage(request.getReceiver().getTelegramId(), confirmationMessage.toString());
            } else {
                // Уведомляем текущего пользователя о подтверждении с его стороны
                messageSender.sendTextMessage(
                        chatId, 
                        "✅ Вы подтвердили встречу. Ожидаем подтверждения от собеседника.");
            }
        } else {
            messageSender.sendTextMessage(
                    chatId, 
                    "⚠️ Произошла ошибка при подтверждении встречи. Пожалуйста, попробуйте снова.");
        }
        
        // Очищаем состояние и данные о выборе места
        stateManager.setUserState(chatId, UserStateManager.UserState.NONE);
        stateManager.clearMeetingPlaceData(chatId);
    }
}
