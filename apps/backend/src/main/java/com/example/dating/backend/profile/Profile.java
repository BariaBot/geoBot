package com.example.dating.backend.profile;

import com.example.dating.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birthday;
    @Column(columnDefinition = "text")
    private String bio;
    private Long photoId;
    private boolean isVip;
    private OffsetDateTime vipUntil;
    private String city;

    public enum Gender { MALE, FEMALE, OTHER }
}
