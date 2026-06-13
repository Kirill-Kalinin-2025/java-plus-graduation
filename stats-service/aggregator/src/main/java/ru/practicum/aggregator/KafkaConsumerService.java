package ru.practicum.aggregator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final AggregationStores stores;
    private final SimilarityCalculator similarityCalculator;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private static final Map<String, Double> ACTION_WEIGHTS = Map.of(
            "VIEW", 0.4,
            "REGISTER", 0.8,
            "LIKE", 1.0
    );

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            UserActionAvro action = objectMapper.readValue(json, UserActionAvro.class);
            log.info("Received action: userId={}, eventId={}, type={}", action.getUserId(), action.getEventId(), action.getActionType());

            long eventId = action.getEventId();
            long userId = action.getUserId();
            double newWeight = ACTION_WEIGHTS.getOrDefault(action.getActionType().name(), 0.4);
            Double oldWeight = stores.getWeight(eventId, userId);

            if (oldWeight == null || newWeight > oldWeight) {
                stores.putWeight(eventId, userId, newWeight);
                similarityCalculator.recalculateSimilarities(eventId);
            }
        } catch (Exception e) {
            log.error("Failed to process message", e);
        }
    }
}