package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.entity.UserAction;

import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    List<UserAction> findByUserId(Long userId);

    // Добавляем метод для поиска по userId и eventId (эффективный)
    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ua FROM UserAction ua WHERE ua.userId = :userId ORDER BY ua.timestamp DESC")
    List<UserAction> findRecentByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(ua.maxWeight), 0) FROM UserAction ua WHERE ua.eventId = :eventId")
    Double sumMaxWeightByEventId(@Param("eventId") Long eventId);

    @Query("SELECT ua.eventId, COALESCE(SUM(ua.maxWeight), 0) FROM UserAction ua WHERE ua.eventId IN :eventIds GROUP BY ua.eventId")
    List<Object[]> sumMaxWeightByEventIdsGrouped(@Param("eventIds") List<Long> eventIds);
}