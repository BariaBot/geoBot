package ru.gang.datingBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "meeting_places")
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String address;
    
    @Column(length = 500)
    private String description;
    
    private Double latitude;
    private Double longitude;
    private Double rating;
    private String photoUrl;
    
    @Column(name = "place_id")
    private String placeId; // ID места в Яндекс.Картах
    
    @Column(length = 1000)
    private String fullInfo; // Дополнительная информация
}
