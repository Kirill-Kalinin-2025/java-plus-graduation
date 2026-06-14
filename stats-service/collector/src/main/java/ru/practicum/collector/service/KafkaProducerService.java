package ru.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private static final String TOPIC = "stats.user-actions.v1";

    public void sendUserAction(UserActionProto proto) {
        try {
            UserActionAvro avro = UserActionAvro.newBuilder()
                    .setUserId(proto.getUserId())
                    .setEventId(proto.getEventId())
                    .setActionType(mapActionType(proto.getActionType()))
                    .setTimestamp(Instant.ofEpochMilli(
                            proto.getTimestamp().getSeconds() * 1000
                                    + proto.getTimestamp().getNanos() / 1_000_000))
                    .build();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            DatumWriter<UserActionAvro> writer = new SpecificDatumWriter<>(UserActionAvro.class);
            writer.write(avro, encoder);
            encoder.flush();
            byte[] bytes = out.toByteArray();

            kafkaTemplate.send(TOPIC, bytes);
            log.info("Sent UserActionAvro to topic {}: userId={}, eventId={}, type={}",
                    TOPIC, avro.getUserId(), avro.getEventId(), avro.getActionType());
        } catch (Exception e) {
            log.error("Failed to send UserActionAvro to Kafka: {}", e.getMessage(), e);
        }
    }

    private ActionTypeAvro mapActionType(ru.practicum.ewm.stats.proto.ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action type: " + protoType);
        };
    }
}