package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.entity.EventSimilarity;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId ORDER BY es.score DESC")
    List<EventSimilarity> findByEventId(@Param("eventId") Long eventId);

    // Добавляем метод для поиска по паре (для обновления)
    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);
}