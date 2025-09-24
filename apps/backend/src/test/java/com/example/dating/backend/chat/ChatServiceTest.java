package com.example.dating.backend.chat;

import com.example.dating.backend.match.Match;
import com.example.dating.backend.match.MatchRepository;
import com.example.dating.backend.user.User;
import com.example.dating.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.dating.backend.support.PostgisIntegrationTest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ChatServiceTest extends PostgisIntegrationTest {
    @Autowired ChatService chatService;
    @Autowired UserRepository userRepository;
    @Autowired MatchRepository matchRepository;

    @Test
    void sendAndFetchHistory() {
        User u1 = userRepository.save(User.builder().telegramId(1L).username("a").createdAt(OffsetDateTime.now()).build());
        User u2 = userRepository.save(User.builder().telegramId(2L).username("b").createdAt(OffsetDateTime.now()).build());
        Match match = matchRepository.save(Match.builder().user1Id(u1.getId()).user2Id(u2.getId()).createdAt(OffsetDateTime.now()).build());
        chatService.sendMessage(match.getId(), u1.getTelegramId(), "hi");
        assertEquals(1, chatService.getHistory(match.getId()).size());
    }
}
