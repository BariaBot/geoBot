package com.example.dating.backend.geo;

import com.example.dating.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Entity
@Table(name = "geo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoLocation {
    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point point;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
