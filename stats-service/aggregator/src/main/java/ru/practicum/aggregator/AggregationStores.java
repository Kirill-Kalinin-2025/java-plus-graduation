package ru.practicum.aggregator;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AggregationStores {

    @Getter
    private final Map<Long, Map<Long, Double>> userWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    public Double getWeight(Long eventId, Long userId) {
        return userWeights.getOrDefault(eventId, Map.of()).get(userId);
    }

    public void putWeight(Long eventId, Long userId, Double weight) {
        userWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>()).put(userId, weight);
    }

    public Double getEventWeightSum(Long eventId) {
        return eventWeightSums.getOrDefault(eventId, 0.0);
    }

    public void putEventWeightSum(Long eventId, Double sum) {
        eventWeightSums.put(eventId, sum);
    }

    public Double getMinWeightSum(Long eventA, Long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
    }

    public void putMinWeightSum(Long eventA, Long eventB, Double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>()).put(second, sum);
    }

    public Map<Long, Map<Long, Double>> getMinWeightsSums() {
        return minWeightsSums;
    }

    // Добавляем метод для получения всех мероприятий
    public Set<Long> getAllEventIds() {
        return userWeights.keySet();
    }
}