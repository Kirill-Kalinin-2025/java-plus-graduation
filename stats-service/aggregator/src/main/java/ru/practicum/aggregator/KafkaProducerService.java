package ru.practicum.aggregator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TOPIC = "stats.events-similarity.v1";

    public void sendSimilarity(Long eventAId, Long eventBId, double score) {
        try {
            long eventA = Math.min(eventAId, eventBId);
            long eventB = Math.max(eventAId, eventBId);

            EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(score)
                    .setTimestamp(Instant.now())
                    .build();

            String json = objectMapper.writeValueAsString(avro);
            kafkaTemplate.send(TOPIC, eventA + "-" + eventB, json);
            log.info("Sent similarity: eventA={}, eventB={}, score={}", eventA, eventB, score);
        } catch (Exception e) {
            log.error("Failed to send similarity", e);
        }
    }
}