package ru.practicum.aggregator;

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

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private static final String TOPIC = "stats.events-similarity.v1";

    public void sendSimilarity(Long eventAId, Long eventBId, double score) {
        long eventA = Math.min(eventAId, eventBId);
        long eventB = Math.max(eventAId, eventBId);

        EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(score)
                .setTimestamp(Instant.now())
                .build();

        kafkaTemplate.send(TOPIC, avro);
        log.info("Sent similarity: eventA={}, eventB={}, score={}", eventA, eventB, score);
    }
}