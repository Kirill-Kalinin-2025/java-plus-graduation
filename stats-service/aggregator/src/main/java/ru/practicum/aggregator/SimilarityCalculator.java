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

    public void processAction(Long eventId, Long userId, String actionType) {
        double newWeight = ACTION_WEIGHTS.getOrDefault(actionType, 0.4);
        double oldWeight = stores.getUserWeightForEvent(userId, eventId);

        log.info("Processing: userId={}, eventId={}, type={}, weight={}",
                userId, eventId, actionType, newWeight);

        // Если новый вес не больше старого, пропускаем
        if (newWeight <= oldWeight) {
            log.info("Skipping: newWeight={} <= oldWeight={}", newWeight, oldWeight);
            return;
        }

        // Обновляем вес пользователя для мероприятия
        stores.putUserWeight(userId, eventId, newWeight);

        // Обновляем сумму весов мероприятия (дельта)
        double deltaSum = newWeight - oldWeight;
        stores.addToEventSum(eventId, deltaSum);

        // Получаем все мероприятия пользователя
        Map<Long, Double> userEvents = stores.getUserWeights(userId);

        // Пересчитываем сходство с другими мероприятиями пользователя
        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {
            Long otherEventId = entry.getKey();
            if (otherEventId.equals(eventId)) continue;

            double otherWeight = entry.getValue();

            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double deltaMin = newMin - oldMin;

            // Обновляем минимальную сумму
            stores.addToMinSum(eventId, otherEventId, deltaMin);

            long first = Math.min(eventId, otherEventId);
            long second = Math.max(eventId, otherEventId);

            double minSum = stores.getMinSum(eventId, otherEventId);
            double sumA = stores.getEventSum(first);
            double sumB = stores.getEventSum(second);
            double similarity = minSum / (Math.sqrt(sumA) * Math.sqrt(sumB));

            double roundedSimilarity = Math.round(similarity * 1000000.0) / 1000000.0;

            log.info("Updating pair ({},{}): deltaMin={}, similarity={}", first, second, deltaMin, roundedSimilarity);

            kafkaProducerService.sendSimilarity(first, second, roundedSimilarity);
        }
    }
}