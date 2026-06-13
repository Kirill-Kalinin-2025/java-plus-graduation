package ru.practicum.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final SimilarityCalculator similarityCalculator;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator")
    public void consume(ConsumerRecord<String, byte[]> record) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(record.value());
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
            SpecificDatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.class);
            UserActionAvro action = reader.read(null, decoder);

            log.info("Received action: userId={}, eventId={}, type={}",
                    action.getUserId(), action.getEventId(), action.getActionType());

            similarityCalculator.processAction(
                    action.getEventId(),
                    action.getUserId(),
                    action.getActionType().name()
            );
        } catch (Exception e) {
            log.error("Failed to deserialize UserActionAvro: {}", e.getMessage(), e);
        }
    }
}