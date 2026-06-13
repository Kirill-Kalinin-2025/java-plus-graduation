package ru.practicum.aggregator;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AggregationStores {

    // userId -> (eventId -> maxWeight)
    private final Map<Long, Map<Long, Double>> userMaxWeights = new ConcurrentHashMap<>();

    // (eventA, eventB) -> minSum, где eventA < eventB
    private final Map<Long, Map<Long, Double>> minSums = new ConcurrentHashMap<>();

    // eventId -> сумма всех весов
    private final Map<Long, Double> eventSums = new ConcurrentHashMap<>();

    public Map<Long, Double> getUserWeights(Long userId) {
        return userMaxWeights.getOrDefault(userId, Map.of());
    }

    public Double getUserWeightForEvent(Long userId, Long eventId) {
        return userMaxWeights.getOrDefault(userId, Map.of()).getOrDefault(eventId, 0.0);
    }

    public void putUserWeight(Long userId, Long eventId, Double weight) {
        userMaxWeights.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(eventId, weight);
    }

    public void addToEventSum(Long eventId, Double delta) {
        eventSums.merge(eventId, delta, Double::sum);
    }

    public Double getEventSum(Long eventId) {
        return eventSums.getOrDefault(eventId, 0.0);
    }

    public void addToMinSum(Long eventA, Long eventB, Double delta) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
    }

    public Double getMinSum(Long eventA, Long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
    }
}