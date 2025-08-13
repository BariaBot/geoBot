package com.example.dating.backend.meeting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRequestRepository extends JpaRepository<MeetingRequest, Long> {
    List<MeetingRequest> findByReceiverIdAndStatus(Long receiverId, MeetingRequest.Status status);
}
