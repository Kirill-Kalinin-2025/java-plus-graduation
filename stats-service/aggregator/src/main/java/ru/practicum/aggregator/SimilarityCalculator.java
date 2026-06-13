package ru.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityCalculator {

    private final AggregationStores stores;
    private final KafkaProducerService kafkaProducerService;

    public void recalculateSimilarities(Long eventA) {
        Map<Long, Double> usersA = stores.getUserWeights().get(eventA);
        if (usersA == null) return;

        double sumA = usersA.values().stream().mapToDouble(Double::doubleValue).sum();
        stores.putEventWeightSum(eventA, sumA);

        for (Long eventB : stores.getUserWeights().keySet()) {
            if (eventB.equals(eventA)) continue;

            Map<Long, Double> usersB = stores.getUserWeights().get(eventB);
            if (usersB == null) continue;

            double minSum = 0.0;
            for (Map.Entry<Long, Double> entry : usersA.entrySet()) {
                Long userId = entry.getKey();
                Double weightA = entry.getValue();
                Double weightB = usersB.get(userId);
                if (weightB != null) {
                    minSum += Math.min(weightA, weightB);
                }
            }

            stores.putMinWeightSum(eventA, eventB, minSum);

            double sumB = stores.getEventWeightSum(eventB);
            if (sumB == 0) {
                sumB = usersB.values().stream().mapToDouble(Double::doubleValue).sum();
                stores.putEventWeightSum(eventB, sumB);
            }

            double similarity = 0.0;
            if (sumA > 0 && sumB > 0) {
                similarity = minSum / Math.sqrt(sumA * sumB);
            }

            kafkaProducerService.sendSimilarity(eventA, eventB, similarity);
        }
    }
}