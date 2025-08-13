package com.example.dating.backend.chat;

import com.example.dating.backend.match.MatchRepository;
import com.example.dating.backend.user.User;
import com.example.dating.backend.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ChatService {
    private final ChatMessageRepository chatRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public ChatService(ChatMessageRepository chatRepository, MatchRepository matchRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }

    public List<ChatMessage> getHistory(Long matchId) {
        return chatRepository.findByMatchIdOrderByCreatedAtAsc(matchId);
    }

    public ChatMessage sendMessage(Long matchId, Long fromTelegramId, String body) {
        matchRepository.findById(matchId).orElseThrow();
        User user = userRepository.findByTelegramId(fromTelegramId).orElseThrow();
        ChatMessage msg = ChatMessage.builder()
                .matchId(matchId)
                .fromUserId(user.getId())
                .body(body)
                .createdAt(OffsetDateTime.now())
                .build();
        return chatRepository.save(msg);
    }
}
