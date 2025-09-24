package com.example.dating.backend.meeting;

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
class MeetingServiceTest extends PostgisIntegrationTest {

    @Autowired
    MeetingService meetingService;
    @Autowired
    MeetingRequestRepository meetingRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    void sendAndAcceptFlow() {
        User sender = userRepository.save(User.builder().telegramId(1L).username("a").createdAt(OffsetDateTime.now()).build());
        User receiver = userRepository.save(User.builder().telegramId(2L).username("b").createdAt(OffsetDateTime.now()).build());

        MeetingService.MeetingRequestDto dto = new MeetingService.MeetingRequestDto(receiver.getId(), OffsetDateTime.now(), "cafe", "hi");
        MeetingRequest req = meetingService.sendRequest(sender.getTelegramId(), dto);
        assertEquals(MeetingRequest.Status.PENDING, req.getStatus());

        meetingService.accept(req.getId(), receiver.getTelegramId());
        MeetingRequest saved = meetingRepository.findById(req.getId()).orElseThrow();
        assertEquals(MeetingRequest.Status.ACCEPTED, saved.getStatus());
    }
}
