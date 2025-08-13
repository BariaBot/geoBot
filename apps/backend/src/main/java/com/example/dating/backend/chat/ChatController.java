package com.example.dating.backend.chat;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/api/chat/{matchId}/history")
    public List<ChatMessage> history(@PathVariable Long matchId) {
        return chatService.getHistory(matchId);
    }

    @MessageMapping("/chat/{matchId}")
    public void handle(@DestinationVariable Long matchId, @Payload ChatPayload payload, Principal principal) {
        Long telegramId = Long.valueOf(principal.getName());
        ChatMessage saved = chatService.sendMessage(matchId, telegramId, payload.body());
        messagingTemplate.convertAndSend("/topic/chat/" + matchId, saved);
    }

    public record ChatPayload(String body) {}
}
