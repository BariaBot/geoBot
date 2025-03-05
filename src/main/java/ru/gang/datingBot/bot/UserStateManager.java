package ru.gang.datingBot.bot;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import ru.gang.datingBot.model.Place;

/**
 * Класс для управления состояниями пользователей в процессе диалога с ботом
 */
public class UserStateManager {

  /**
   * Enum для отслеживания состояния разговора с пользователями
   */
  public enum UserState {
    NONE,
    WAITING_FOR_DESCRIPTION,
    WAITING_FOR_INTERESTS,
    WAITING_FOR_PHOTO,
    WAITING_FOR_AGE,
    WAITING_FOR_GENDER,
    WAITING_FOR_MIN_AGE,
    WAITING_FOR_MAX_AGE,
    WAITING_FOR_GENDER_PREFERENCE,
    WAITING_FOR_MEETING_MESSAGE,
    WAITING_FOR_MEETING_PHOTO,
    CHATTING, // Состояние чата
    WAITING_FOR_PLACE_TYPE,    // Ожидание ввода типа заведения
    VIEWING_PLACES,            // Просмотр мест
    WAITING_FOR_DATE,          // Выбор даты встречи
    WAITING_FOR_TIME,          // Выбор времени встречи
    WAITING_FOR_CONFIRMATION   // Подтверждение встречи
  }
  
  // Карта для отслеживания текущего состояния каждого пользователя
  private final Map<Long, UserState> userStates = new HashMap<>();
  
  // Хранилище для кэширования результатов поиска
  private final Map<Long, List<ru.gang.datingBot.model.User>> nearbyUsersCache = new HashMap<>();
  private final Map<Long, Integer> currentUserIndexCache = new HashMap<>();
  
  // Хранилище временных данных для создания запроса на встречу
  private final Map<Long, String> meetingRequestMessages = new HashMap<>();
  private final Map<Long, String> meetingRequestPhotos = new HashMap<>();
  private final Map<Long, Long> meetingRequestTargets = new HashMap<>();
  
  // Хранилище настроек геолокации
  private final Map<Long, Integer> userLiveLocationDurations = new HashMap<>();
  private final Map<Long, Integer> userSearchRadius = new HashMap<>();
  
  // Хранилище информации о текущем чате
  private final Map<Long, Long> currentChatUser = new HashMap<>(); // chatId -> targetUserId
  private final Map<Long, Long> currentChatMeetingRequest = new HashMap<>(); // chatId -> meetingRequestId

  // Хранилище для информации о местах встречи
  private final Map<Long, List<Place>> cachedPlaces = new HashMap<>();
  private final Map<Long, Integer> currentPlaceIndex = new HashMap<>();
  private final Map<Long, Long> selectedPlaceId = new HashMap<>();
  private final Map<Long, String> selectedDate = new HashMap<>();
  private final Map<Long, String> selectedTime = new HashMap<>();
  private final Map<Long, Long> pendingMeetingRequestId = new HashMap<>();

  /**
   * Получить текущее состояние пользователя
   */
  public UserState getUserState(Long chatId) {
    return userStates.getOrDefault(chatId, UserState.NONE);
  }

  /**
   * Установить состояние пользователя
   */
  public void setUserState(Long chatId, UserState state) {
    userStates.put(chatId, state);
  }

  /**
   * Проверить, находится ли пользователь в указанном состоянии
   */
  public boolean isUserInState(Long chatId, UserState state) {
    return getUserState(chatId) == state;
  }

  /**
   * Начать чат между пользователями
   */
  public void startChatting(Long chatId, Long targetUserId, Long meetingRequestId) {
    setUserState(chatId, UserState.CHATTING);
    currentChatUser.put(chatId, targetUserId);
    currentChatMeetingRequest.put(chatId, meetingRequestId);
  }

  /**
   * Завершить текущий чат
   */
  public void endChatting(Long chatId) {
    setUserState(chatId, UserState.NONE);
    currentChatUser.remove(chatId);
    currentChatMeetingRequest.remove(chatId);
  }
  
  /**
   * Получить ID пользователя, с которым ведется чат
   */
  public Long getCurrentChatUser(Long chatId) {
    return currentChatUser.get(chatId);
  }
  
  /**
   * Получить ID запроса на встречу, связанного с чатом
   */
  public Long getCurrentChatMeetingRequest(Long chatId) {
    return currentChatMeetingRequest.get(chatId);
  }

  // Методы для кэша поиска
  public void cacheNearbyUsers(Long chatId, List<ru.gang.datingBot.model.User> users) {
    nearbyUsersCache.put(chatId, users);
    currentUserIndexCache.put(chatId, 0); // Начинаем с первого пользователя
  }

  public List<ru.gang.datingBot.model.User> getNearbyUsersCache(Long chatId) {
    return nearbyUsersCache.get(chatId);
  }

  public Integer getCurrentUserIndex(Long chatId) {
    return currentUserIndexCache.get(chatId);
  }

  public void setCurrentUserIndex(Long chatId, Integer index) {
    currentUserIndexCache.put(chatId, index);
  }

  /**
   * Методы для управления запросами на встречу
   */
  public void saveMeetingRequestMessage(Long chatId, String message) {
    meetingRequestMessages.put(chatId, message);
  }

  public String getMeetingRequestMessage(Long chatId) {
    return meetingRequestMessages.get(chatId);
  }

  public void saveMeetingRequestPhoto(Long chatId, String photoFileId) {
    meetingRequestPhotos.put(chatId, photoFileId);
  }

  public String getMeetingRequestPhoto(Long chatId) {
    return meetingRequestPhotos.get(chatId);
  }

  public void saveMeetingRequestTarget(Long chatId, Long targetId) {
    meetingRequestTargets.put(chatId, targetId);
  }

  public Long getMeetingRequestTarget(Long chatId) {
    return meetingRequestTargets.get(chatId);
  }

  public void clearMeetingRequestData(Long chatId) {
    meetingRequestMessages.remove(chatId);
    meetingRequestPhotos.remove(chatId);
    meetingRequestTargets.remove(chatId);
  }

  /**
   * Методы для управления настройками геолокации
   */
  public void saveLocationDuration(Long chatId, Integer hours) {
    userLiveLocationDurations.put(chatId, hours);
  }

  public Integer getLocationDuration(Long chatId) {
    return userLiveLocationDurations.get(chatId);
  }

  public void saveSearchRadius(Long chatId, Integer radius) {
    userSearchRadius.put(chatId, radius);
  }

  public Integer getSearchRadius(Long chatId) {
    return userSearchRadius.get(chatId);
  }

  public boolean hasLocationSettings(Long chatId) {
    return userLiveLocationDurations.containsKey(chatId) && userSearchRadius.containsKey(chatId);
  }

  /**
   * Методы для работы с местами встреч
   */
  public void cachePlaces(Long chatId, List<Place> places) {
    cachedPlaces.put(chatId, places);
    currentPlaceIndex.put(chatId, 0);
  }

  public List<Place> getCachedPlaces(Long chatId) {
    return cachedPlaces.get(chatId);
  }

  public Integer getCurrentPlaceIndex(Long chatId) {
    return currentPlaceIndex.getOrDefault(chatId, 0);
  }

  public void setCurrentPlaceIndex(Long chatId, Integer index) {
    currentPlaceIndex.put(chatId, index);
  }

  public void saveSelectedPlaceId(Long chatId, Long placeId) {
    selectedPlaceId.put(chatId, placeId);
  }

  public Long getSelectedPlaceId(Long chatId) {
    return selectedPlaceId.get(chatId);
  }

  public void saveSelectedDate(Long chatId, String date) {
    selectedDate.put(chatId, date);
  }

  public String getSelectedDate(Long chatId) {
    return selectedDate.get(chatId);
  }

  public void saveSelectedTime(Long chatId, String time) {
    selectedTime.put(chatId, time);
  }

  public String getSelectedTime(Long chatId) {
    return selectedTime.get(chatId);
  }

  public void savePendingMeetingRequestId(Long chatId, Long meetingRequestId) {
    pendingMeetingRequestId.put(chatId, meetingRequestId);
  }

  public Long getPendingMeetingRequestId(Long chatId) {
    return pendingMeetingRequestId.get(chatId);
  }

  public void clearMeetingPlaceData(Long chatId) {
    cachedPlaces.remove(chatId);
    currentPlaceIndex.remove(chatId);
    selectedPlaceId.remove(chatId);
    selectedDate.remove(chatId);
    selectedTime.remove(chatId);
    pendingMeetingRequestId.remove(chatId);
  }
}
