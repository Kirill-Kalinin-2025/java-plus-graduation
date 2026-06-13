package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.entity.EventSimilarity;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    private static final int MAX_RECENT_EVENTS = 10;  // K для недавних событий
    private static final int MAX_NEIGHBORS = 10;      // K для ближайших соседей

    /**
     * Получение рекомендаций для пользователя на основе предсказания оценки
     */
    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        // 1. Получаем последние MAX_RECENT_EVENTS мероприятий пользователя
        List<UserAction> recentActions = userActionRepository.findRecentByUserId(userId);
        if (recentActions.isEmpty()) {
            log.debug("No recent actions for user {}", userId);
            return Stream.empty();
        }

        List<UserAction> topRecent = recentActions.stream()
                .limit(MAX_RECENT_EVENTS)
                .toList();

        // 2. Множество мероприятий, с которыми пользователь уже взаимодействовал
        Set<Long> interactedEventIds = recentActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        // 3. Находим кандидатов (мероприятия, похожие на topRecent, но не взаимодействованные)
        Map<Long, Double> candidateMaxSimilarity = new HashMap<>();

        for (UserAction action : topRecent) {
            List<EventSimilarity> similarities = eventSimilarityRepository.findByEventId(action.getEventId());
            for (EventSimilarity sim : similarities) {
                Long candidateId = sim.getEventA().equals(action.getEventId()) ? sim.getEventB() : sim.getEventA();

                // Пропускаем уже взаимодействованные мероприятия
                if (interactedEventIds.contains(candidateId)) {
                    continue;
                }

                // Сохраняем максимальное сходство для каждого кандидата
                double currentMax = candidateMaxSimilarity.getOrDefault(candidateId, 0.0);
                if (sim.getScore() > currentMax) {
                    candidateMaxSimilarity.put(candidateId, sim.getScore());
                }
            }
        }

        if (candidateMaxSimilarity.isEmpty()) {
            log.debug("No candidates found for user {}", userId);
            return Stream.empty();
        }

        // 4. Сортируем кандидатов по убыванию максимального сходства и берём top N
        List<Long> topCandidates = candidateMaxSimilarity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topCandidates.isEmpty()) {
            return Stream.empty();
        }

        // 5. Для каждого кандидата предсказываем оценку
        Map<Long, Double> userRatings = recentActions.stream()
                .collect(Collectors.toMap(UserAction::getEventId, UserAction::getMaxWeight));

        return topCandidates.stream()
                .map(candidateId -> {
                    double predictedScore = predictScore(candidateId, userRatings, interactedEventIds);
                    return RecommendedEventProto.newBuilder()
                            .setEventId(candidateId)
                            .setScore(predictedScore)
                            .build();
                })
                .filter(proto -> proto.getScore() > 0);
    }

    /**
     * Предсказание оценки для мероприятия по формуле:
     * Σ(similarity * user_rating) / Σ(similarity)
     */
    private double predictScore(Long eventId, Map<Long, Double> userRatings, Set<Long> userEvents) {
        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventId(eventId);

        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarity sim : similarities) {
            Long similarEventId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();

            // Используем только те мероприятия, которые пользователь оценил
            if (userRatings.containsKey(similarEventId)) {
                double rating = userRatings.get(similarEventId);
                weightedSum += sim.getScore() * rating;
                similaritySum += sim.getScore();
            }
        }

        if (similaritySum > 0) {
            return weightedSum / similaritySum;
        }
        return 0.0;
    }

    /**
     * Получение похожих мероприятий (исключая те, с которыми пользователь уже взаимодействовал)
     */
    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        List<UserAction> userActions = userActionRepository.findByUserId(userId);
        Set<Long> interactedEventIds = userActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventId(eventId);

        return similarities.stream()
                .map(sim -> {
                    Long similarId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                    return new AbstractMap.SimpleEntry<>(similarId, sim.getScore());
                })
                .filter(e -> !interactedEventIds.contains(e.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getKey())
                        .setScore(e.getValue())
                        .build());
    }

    /**
     * Получение суммы максимальных весов взаимодействий для указанных мероприятий
     */
    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Stream.empty();
        }

        // Используем группирующий запрос для эффективности
        List<Object[]> results = userActionRepository.sumMaxWeightByEventIdsGrouped(eventIds);
        Map<Long, Double> scoreMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));

        return eventIds.stream()
                .map(eventId -> RecommendedEventProto.newBuilder()
                        .setEventId(eventId)
                        .setScore(scoreMap.getOrDefault(eventId, 0.0))
                        .build());
    }
}