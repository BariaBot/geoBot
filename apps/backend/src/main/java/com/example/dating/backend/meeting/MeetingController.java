package com.example.dating.backend.meeting;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingService meetingService;

    @PostMapping("/request")
    public MeetingRequest send(@RequestBody MeetingService.MeetingRequestDto dto, Authentication auth) {
        Long senderTelegramId = Long.valueOf(auth.getName());
        return meetingService.sendRequest(senderTelegramId, dto);
    }

    @PostMapping("/{id}/accept")
    public void accept(@PathVariable Long id, Authentication auth) {
        meetingService.accept(id, Long.valueOf(auth.getName()));
    }

    @PostMapping("/{id}/decline")
    public void decline(@PathVariable Long id, Authentication auth) {
        meetingService.decline(id, Long.valueOf(auth.getName()));
    }

    @PostMapping("/{id}/complete")
    public void complete(@PathVariable Long id, Authentication auth) {
        meetingService.complete(id, Long.valueOf(auth.getName()));
    }

    @GetMapping("/pending")
    public List<MeetingRequest> pending(Authentication auth) {
        return meetingService.pending(Long.valueOf(auth.getName()));
    }
}
