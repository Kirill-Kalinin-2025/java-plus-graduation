package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.entity.UserAction;

import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    List<UserAction> findByUserId(Long userId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ua FROM UserAction ua WHERE ua.userId = :userId ORDER BY ua.timestamp DESC")
    List<UserAction> findRecentByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(ua.maxWeight), 0) FROM UserAction ua WHERE ua.eventId = :eventId")
    Double sumMaxWeightByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(ua.maxWeight), 0) FROM UserAction ua WHERE ua.eventId IN :eventIds GROUP BY ua.eventId")
    List<Double> sumMaxWeightByEventIds(@Param("eventIds") List<Long> eventIds);
}