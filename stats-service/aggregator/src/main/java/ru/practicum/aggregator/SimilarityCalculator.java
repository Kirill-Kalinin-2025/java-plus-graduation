package ru.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityCalculator {

    private final AggregationStores stores;
    private final KafkaProducerService kafkaProducerService;

    private static final Map<String, Double> ACTION_WEIGHTS = Map.of(
            "VIEW", 0.4,
            "REGISTER", 0.8,
            "LIKE", 1.0
    );

    // Храним последние вычисленные сходства, чтобы не отправлять дубликаты
    private final Map<Long, Map<Long, Double>> lastSentSimilarities = new ConcurrentHashMap<>();

    /**
     * Обновляет веса пользователя для мероприятия и пересчитывает сходства
     * @param eventId мероприятие, для которого пришло новое действие
     * @param userId пользователь, совершивший действие
     * @param actionType тип действия
     * @return true если вес был обновлён, false если нет
     */
    public boolean updateUserWeightAndRecalculate(Long eventId, Long userId, String actionType) {
        double newWeight = ACTION_WEIGHTS.getOrDefault(actionType, 0.4);
        Double oldWeight = stores.getWeight(eventId, userId);

        // Если вес не изменился, ничего не делаем
        if (oldWeight != null && newWeight <= oldWeight) {
            log.debug("Weight not changed for eventId={}, userId={}, oldWeight={}, newWeight={}",
                    eventId, userId, oldWeight, newWeight);
            return false;
        }

        // Обновляем вес
        stores.putWeight(eventId, userId, newWeight);
        log.info("Updated weight for eventId={}, userId={}, oldWeight={}, newWeight={}",
                eventId, userId, oldWeight, newWeight);

        // Пересчитываем сумму весов для eventId
        Map<Long, Double> usersForEvent = stores.getUserWeights().get(eventId);
        if (usersForEvent != null) {
            double sum = usersForEvent.values().stream().mapToDouble(Double::doubleValue).sum();
            stores.putEventWeightSum(eventId, sum);
        }

        // Пересчитываем сходство только для пар с участием eventId
        recalculateSimilaritiesForEvent(eventId);

        return true;
    }

    /**
     * Пересчитывает сходство для всех пар, где участвует eventId
     */
    private void recalculateSimilaritiesForEvent(Long eventId) {
        Map<Long, Double> usersForEvent = stores.getUserWeights().get(eventId);
        if (usersForEvent == null || usersForEvent.isEmpty()) {
            log.debug("No users for eventId={}", eventId);
            return;
        }

        double sumA = stores.getEventWeightSum(eventId);
        log.debug("Recalculating similarities for eventId={}, sumA={}, usersCount={}",
                eventId, sumA, usersForEvent.size());

        // Перебираем все другие мероприятия, у которых есть пользователи
        for (Map.Entry<Long, Map<Long, Double>> entry : stores.getUserWeights().entrySet()) {
            Long otherEventId = entry.getKey();

            // Пропускаем само себя
            if (otherEventId.equals(eventId)) {
                continue;
            }

            Map<Long, Double> usersForOther = entry.getValue();
            if (usersForOther == null || usersForOther.isEmpty()) {
                continue;
            }

            // Вычисляем S_min для пары (сумма минимальных весов общих пользователей)
            double minSum = 0.0;
            for (Map.Entry<Long, Double> userWeightA : usersForEvent.entrySet()) {
                Long userId = userWeightA.getKey();
                Double weightA = userWeightA.getValue();
                Double weightB = usersForOther.get(userId);
                if (weightB != null) {
                    minSum += Math.min(weightA, weightB);
                }
            }

            // Сохраняем сумму минимальных весов
            stores.putMinWeightSum(eventId, otherEventId, minSum);

            // Получаем или вычисляем сумму весов для otherEventId
            double sumB = stores.getEventWeightSum(otherEventId);
            if (sumB == 0.0) {
                sumB = usersForOther.values().stream().mapToDouble(Double::doubleValue).sum();
                stores.putEventWeightSum(otherEventId, sumB);
            }

            // Вычисляем косинусное сходство
            double similarity = 0.0;
            if (sumA > 0 && sumB > 0 && minSum > 0) {
                similarity = minSum / Math.sqrt(sumA * sumB);
            }

            // Отправляем только если сходство изменилось
            Double lastSimilarity = getLastSimilarity(eventId, otherEventId);
            if (lastSimilarity == null || Math.abs(lastSimilarity - similarity) > 0.0001) {
                kafkaProducerService.sendSimilarity(eventId, otherEventId, similarity);
                saveLastSimilarity(eventId, otherEventId, similarity);
                log.debug("Sent similarity: eventA={}, eventB={}, score={}, oldScore={}",
                        eventId, otherEventId, similarity, lastSimilarity);
            } else {
                log.debug("Similarity unchanged for eventA={}, eventB={}, score={}",
                        eventId, otherEventId, similarity);
            }
        }
    }

    /**
     * Получает последнее отправленное сходство
     */
    private Double getLastSimilarity(Long eventA, Long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return lastSentSimilarities.getOrDefault(first, Map.of()).get(second);
    }

    /**
     * Сохраняет последнее отправленное сходство
     */
    private void saveLastSimilarity(Long eventA, Long eventB, Double similarity) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        lastSentSimilarities.computeIfAbsent(first, k -> new ConcurrentHashMap<>()).put(second, similarity);
    }
}