package com.example.dating.backend.meeting;

import com.example.dating.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRequestRepository meetingRepository;
    private final UserRepository userRepository;

    public MeetingRequest sendRequest(Long senderTelegramId, MeetingRequestDto dto) {
        Long senderId = userRepository.findByTelegramId(senderTelegramId)
                .orElseThrow().getId();
        MeetingRequest request = MeetingRequest.builder()
                .senderId(senderId)
                .receiverId(dto.receiverId())
                .whenTs(dto.whenTs())
                .place(dto.place())
                .note(dto.note())
                .status(MeetingRequest.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();
        return meetingRepository.save(request);
    }

    public void accept(Long requestId, Long receiverTelegramId) {
        updateStatus(requestId, receiverTelegramId, MeetingRequest.Status.ACCEPTED);
    }

    public void decline(Long requestId, Long receiverTelegramId) {
        updateStatus(requestId, receiverTelegramId, MeetingRequest.Status.DECLINED);
    }

    public void complete(Long requestId, Long userTelegramId) {
        MeetingRequest req = meetingRepository.findById(requestId).orElseThrow();
        Long userId = userRepository.findByTelegramId(userTelegramId).orElseThrow().getId();
        if (!req.getSenderId().equals(userId) && !req.getReceiverId().equals(userId)) {
            throw new IllegalStateException("Forbidden");
        }
        req.setStatus(MeetingRequest.Status.COMPLETED);
        meetingRepository.save(req);
    }

    public List<MeetingRequest> pending(Long receiverTelegramId) {
        Long receiverId = userRepository.findByTelegramId(receiverTelegramId)
                .orElseThrow().getId();
        return meetingRepository.findByReceiverIdAndStatus(receiverId, MeetingRequest.Status.PENDING);
    }

    private void updateStatus(Long requestId, Long receiverTelegramId, MeetingRequest.Status status) {
        MeetingRequest req = meetingRepository.findById(requestId).orElseThrow();
        Long receiverId = userRepository.findByTelegramId(receiverTelegramId).orElseThrow().getId();
        if (!req.getReceiverId().equals(receiverId)) {
            throw new IllegalStateException("Forbidden");
        }
        req.setStatus(status);
        meetingRepository.save(req);
    }

    public record MeetingRequestDto(Long receiverId, OffsetDateTime whenTs, String place, String note) {}
}
