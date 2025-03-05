package ru.gang.datingBot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.gang.datingBot.dto.YandexPlaceResponse;
import ru.gang.datingBot.model.Place;
import ru.gang.datingBot.model.User;
import ru.gang.datingBot.repository.PlaceRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {

    @Value("${yandex.api.key:test_api_key}")
    private String apiKey;
    
    private final RestTemplate restTemplate;
    private final PlaceRepository placeRepository;
    
    /**
     * Находит места между двумя пользователями
     */
    public List<Place> findPlacesBetweenUsers(User user1, User user2, String placeType, int radius) {
        // Находим среднюю точку между пользователями
        double midLat = (user1.getLatitude() + user2.getLatitude()) / 2;
        double midLon = (user1.getLongitude() + user2.getLongitude()) / 2;
        
        return searchPlacesByTypeAndLocation(placeType, midLat, midLon, radius);
    }
    
    /**
     * Поиск мест по типу и локации через API Яндекс
     */
    private List<Place> searchPlacesByTypeAndLocation(String type, double lat, double lon, int radius) {
        String url = String.format(
            "https://search-maps.yandex.ru/v1/?apikey=%s&text=%s&lang=ru_RU&ll=%.6f,%.6f&spn=0.1,0.1&results=10",
            apiKey, type, lon, lat
        );
        
        try {
            ResponseEntity<YandexPlaceResponse> response = 
                restTemplate.getForEntity(url, YandexPlaceResponse.class);
                
            if (response.getBody() == null || response.getBody().getFeatures() == null) {
                return new ArrayList<>();
            }
            
            return convertYandexResponseToPlaces(response.getBody());
        } catch (Exception e) {
            System.out.println("Error searching places: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Конвертирует ответ от API в наши объекты Place
     */
    private List<Place> convertYandexResponseToPlaces(YandexPlaceResponse response) {
        List<Place> places = new ArrayList<>();
        
        for (var feature : response.getFeatures()) {
            var props = feature.getProperties();
            var geom = feature.getGeometry();
            
            if (props != null && geom != null && geom.getCoordinates() != null) {
                Place place = new Place();
                place.setName(props.getName());
                place.setAddress(props.getAddress());
                place.setDescription(props.getDescription());
                place.setRating(props.getRating());
                place.setPhotoUrl(props.getPhotoUrl());
                place.setPlaceId(props.getCompanyId());
                
                // Координаты в Яндекс API хранятся в формате [lon, lat]
                double[] coords = geom.getCoordinates();
                if (coords.length >= 2) {
                    place.setLongitude(coords[0]);
                    place.setLatitude(coords[1]);
                }
                
                // Сохранить место в БД
                place = placeRepository.save(place);
                places.add(place);
            }
        }
        
        // Сортируем по рейтингу
        return places.stream()
                .sorted(Comparator.comparing(Place::getRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList());
    }
    
    /**
     * Сохраняет место в БД
     */
    public Place savePlace(Place place) {
        return placeRepository.save(place);
    }
    
    /**
     * Получает место по ID
     */
    public Place getPlaceById(Long id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Место не найдено: " + id));
    }
}
