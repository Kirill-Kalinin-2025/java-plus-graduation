package ru.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private static final String TOPIC = "stats.events-similarity.v1";

    public void sendSimilarity(Long eventA, Long eventB, double score) {
        try {
            EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(score)
                    .setTimestamp(Instant.now())
                    .build();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            DatumWriter<EventSimilarityAvro> writer = new SpecificDatumWriter<>(EventSimilarityAvro.class);
            writer.write(avro, encoder);
            encoder.flush();
            byte[] bytes = out.toByteArray();

            kafkaTemplate.send(TOPIC, bytes);
            log.info("Sent similarity: eventA={}, eventB={}, score={}", eventA, eventB, score);
        } catch (Exception e) {
            log.error("Failed to send EventSimilarityAvro to Kafka: {}", e.getMessage(), e);
        }
    }
}