package ru.gang.datingBot.bot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

public class UserStateManager {

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
    CHATTING
  }
  
  private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
  private final Map<Long, List<ru.gang.datingBot.model.User>> nearbyUsersCache = new ConcurrentHashMap<>();
  private final Map<Long, Integer> currentUserIndexCache = new ConcurrentHashMap<>();
  private final Map<Long, String> meetingRequestMessages = new ConcurrentHashMap<>();
  private final Map<Long, String> meetingRequestPhotos = new ConcurrentHashMap<>();
  private final Map<Long, Long> meetingRequestTargets = new ConcurrentHashMap<>();
  private final Map<Long, Integer> userLiveLocationDurations = new ConcurrentHashMap<>();
  private final Map<Long, Integer> userSearchRadius = new ConcurrentHashMap<>();
  private final Map<Long, Long> currentChatUser = new ConcurrentHashMap<>();
  private final Map<Long, Long> currentChatMeetingRequest = new ConcurrentHashMap<>();

  public UserState getUserState(Long chatId) {
    return userStates.getOrDefault(chatId, UserState.NONE);
  }

  public void setUserState(Long chatId, UserState state) {
    userStates.put(chatId, state);
  }

  public boolean isUserInState(Long chatId, UserState state) {
    return getUserState(chatId) == state;
  }

  public void startChatting(Long chatId, Long targetUserId, Long meetingRequestId) {
    setUserState(chatId, UserState.CHATTING);
    currentChatUser.put(chatId, targetUserId);
    currentChatMeetingRequest.put(chatId, meetingRequestId);
  }

  public void endChatting(Long chatId) {
    setUserState(chatId, UserState.NONE);
    currentChatUser.remove(chatId);
    currentChatMeetingRequest.remove(chatId);
  }
  
  public Long getCurrentChatUser(Long chatId) {
    return currentChatUser.get(chatId);
  }
  
  public Long getCurrentChatMeetingRequest(Long chatId) {
    return currentChatMeetingRequest.get(chatId);
  }

  public void cacheNearbyUsers(Long chatId, List<ru.gang.datingBot.model.User> users) {
    nearbyUsersCache.put(chatId, users);
    currentUserIndexCache.put(chatId, 0);
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
}