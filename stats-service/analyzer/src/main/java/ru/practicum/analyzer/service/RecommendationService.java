package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.entity.EventSimilarity;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    private static final int MAX_NEIGHBORS = 10;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        List<UserAction> recentActions = userActionRepository.findRecentByUserId(userId);
        if (recentActions.isEmpty()) {
            return Stream.empty();
        }

        List<UserAction> topRecent = recentActions.stream()
                .limit(MAX_NEIGHBORS)
                .toList();

        Set<Long> interactedEventIds = recentActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        Map<Long, Double> candidateScores = new HashMap<>();
        Map<Long, Double> maxSimilarity = new HashMap<>();

        for (UserAction action : topRecent) {
            List<EventSimilarity> similarities = eventSimilarityRepository.findByEventId(action.getEventId());
            for (EventSimilarity sim : similarities) {
                Long candidateId = sim.getEventA().equals(action.getEventId()) ? sim.getEventB() : sim.getEventA();
                if (interactedEventIds.contains(candidateId)) continue;

                if (sim.getScore() > maxSimilarity.getOrDefault(candidateId, 0.0)) {
                    maxSimilarity.put(candidateId, sim.getScore());
                    double weightedScore = action.getMaxWeight() * sim.getScore();
                    candidateScores.merge(candidateId, weightedScore, Double::sum);
                }
            }
        }

        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(e -> RecommendedEventProto.newBuilder()
                        .setEventId(e.getKey())
                        .setScore(e.getValue())
                        .build());
    }

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

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        return eventIds.stream()
                .map(eventId -> {
                    Double sum = userActionRepository.sumMaxWeightByEventId(eventId);
                    return RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(sum != null ? sum : 0.0)
                            .build();
                });
    }
}