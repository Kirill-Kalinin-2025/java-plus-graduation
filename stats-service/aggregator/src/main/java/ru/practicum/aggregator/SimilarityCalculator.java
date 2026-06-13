package ru.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

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

    /**
     * Обновляет веса пользователя для мероприятия и пересчитывает сходства
     */
    public boolean updateUserWeightAndRecalculate(Long eventId, Long userId, String actionType) {
        double newWeight = ACTION_WEIGHTS.getOrDefault(actionType, 0.4);
        Double oldWeight = stores.getWeight(eventId, userId);

        // Если вес не изменился (новый вес не больше старого), ничего не делаем
        if (oldWeight != null && newWeight <= oldWeight) {
            log.debug("Weight not changed for eventId={}, userId={}, oldWeight={}, newWeight={}",
                    eventId, userId, oldWeight, newWeight);
            return false;
        }

        // Обновляем вес пользователя для мероприятия
        stores.putWeight(eventId, userId, newWeight);
        log.info("Updated weight for eventId={}, userId={}, oldWeight={}, newWeight={}",
                eventId, userId, oldWeight, newWeight);

        // Пересчитываем сумму весов для eventId
        Map<Long, Double> usersForEvent = stores.getUserWeights().get(eventId);
        double sumA = usersForEvent.values().stream().mapToDouble(Double::doubleValue).sum();
        stores.putEventWeightSum(eventId, sumA);

        // Пересчитываем сходство только для пар (eventId, otherEvent),
        // где у otherEvent тоже есть пользователи
        for (Map.Entry<Long, Map<Long, Double>> entry : stores.getUserWeights().entrySet()) {
            Long otherEventId = entry.getKey();

            // Пропускаем само себя
            if (otherEventId.equals(eventId)) {
                continue;
            }

            Map<Long, Double> usersForOther = entry.getValue();

            // Пересчитываем S_min для пары с учётом нового веса пользователя
            double minSum = 0.0;
            for (Map.Entry<Long, Double> userEntry : usersForEvent.entrySet()) {
                Long uid = userEntry.getKey();
                Double weightA = userEntry.getValue();
                Double weightB = usersForOther.get(uid);
                if (weightB != null) {
                    minSum += Math.min(weightA, weightB);
                }
            }

            // Сохраняем S_min
            stores.putMinWeightSum(eventId, otherEventId, minSum);

            // Получаем сумму весов для otherEvent
            double sumB = usersForOther.values().stream().mapToDouble(Double::doubleValue).sum();
            stores.putEventWeightSum(otherEventId, sumB);

            // Вычисляем косинусное сходство
            double similarity = 0.0;
            if (sumA > 0 && sumB > 0 && minSum > 0) {
                similarity = minSum / Math.sqrt(sumA * sumB);
            }

            // Отправляем сходство всегда, когда есть изменения
            kafkaProducerService.sendSimilarity(eventId, otherEventId, similarity);
            log.info("Sent similarity: eventA={}, eventB={}, score={}",
                    Math.min(eventId, otherEventId), Math.max(eventId, otherEventId), similarity);
        }

        return true;
    }
}