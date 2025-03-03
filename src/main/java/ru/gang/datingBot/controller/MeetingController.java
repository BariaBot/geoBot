package ru.gang.datingBot.controller;

import ru.gang.datingBot.model.MeetingRequest;
import ru.gang.datingBot.service.MeetingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {
  private final MeetingService meetingService;

  public MeetingController(MeetingService meetingService) {
    this.meetingService = meetingService;
  }

  // Получить все активные запросы на встречу для пользователя
  @GetMapping("/{receiverId}/pending")
  public ResponseEntity<List<MeetingRequest>> getPendingRequests(@PathVariable Long receiverId) {
    List<MeetingRequest> requests = meetingService.getPendingRequestsForUser(receiverId);
    return ResponseEntity.ok(requests);
  }

  // Отправить запрос на встречу
  @PostMapping("/send")
  public ResponseEntity<String> sendMeetingRequest(
      @RequestParam Long senderId,
      @RequestParam Long receiverId,
      @RequestParam String message,
      @RequestParam LocalDateTime scheduledTime) {

    meetingService.sendMeetingRequest(senderId, receiverId, message, scheduledTime);
    return ResponseEntity.ok("Запрос на встречу отправлен!");
  }

  // Принять запрос на встречу
  @PostMapping("/{requestId}/accept")
  public ResponseEntity<String> acceptMeetingRequest(@PathVariable Long requestId) {
    meetingService.acceptMeetingRequest(requestId);
    return ResponseEntity.ok("Встреча принята!");
  }

  // Отклонить запрос на встречу
  @PostMapping("/{requestId}/decline")
  public ResponseEntity<String> declineMeetingRequest(@PathVariable Long requestId) {
    meetingService.declineMeetingRequest(requestId);
    return ResponseEntity.ok("Встреча отклонена.");
  }

  // Завершить встречу (бот автоматически отправит запрос на отзыв)
  @PostMapping("/{requestId}/complete")
  public ResponseEntity<String> completeMeeting(@PathVariable Long requestId) {
    meetingService.completeMeeting(requestId);
    return ResponseEntity.ok("Встреча завершена, запрос на отзыв отправлен.");
  }
}
