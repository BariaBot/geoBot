package com.example.dating.backend.meeting;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "meeting_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "when_ts", nullable = false)
    private OffsetDateTime whenTs;

    private String place;
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public enum Status {
        PENDING, ACCEPTED, DECLINED, COMPLETED
    }
}
