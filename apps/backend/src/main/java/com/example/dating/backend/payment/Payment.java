package com.example.dating.backend.payment;

import com.example.dating.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(name = "amount_stars")
    private int amountStars;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "ext_id")
    private String extId;

    private OffsetDateTime createdAt;

    public enum Provider { TELEGRAM_STARS, STUB }
    public enum Status { PENDING, SUCCESS, FAIL }
}
