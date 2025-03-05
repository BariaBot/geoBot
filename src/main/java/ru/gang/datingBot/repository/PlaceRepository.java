package ru.gang.datingBot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.gang.datingBot.model.Place;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {
    // Базовые методы CRUD наследуются от JpaRepository
}
