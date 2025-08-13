package com.example.dating.backend.match;

import com.example.dating.backend.user.User;
import com.example.dating.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MatchServiceTest {

    @Autowired
    MatchService matchService;
    @Autowired
    UserRepository userRepository;

    @Test
    void reciprocalLikesCreateMatch() {
        User u1 = userRepository.save(User.builder().telegramId(1L).username("a").createdAt(OffsetDateTime.now()).build());
        User u2 = userRepository.save(User.builder().telegramId(2L).username("b").createdAt(OffsetDateTime.now()).build());

        assertFalse(matchService.like(1L, u2.getId()));
        assertTrue(matchService.like(2L, u1.getId()));
        assertEquals(1, matchService.getMatches(1L).size());
    }
}
