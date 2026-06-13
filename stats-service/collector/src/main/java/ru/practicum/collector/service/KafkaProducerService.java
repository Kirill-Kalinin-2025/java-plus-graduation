package ru.practicum.collector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOPIC = "stats.user-actions.v1";

    public void sendUserAction(UserActionProto proto) {
        try {
            UserActionAvro avro = UserActionAvro.newBuilder()
                    .setUserId(proto.getUserId())
                    .setEventId(proto.getEventId())
                    .setActionType(mapActionType(proto.getActionType()))
                    .setTimestamp(Instant.ofEpochMilli(
                            proto.getTimestamp().getSeconds() * 1000 + proto.getTimestamp().getNanos() / 1_000_000))
                    .build();

            String json = objectMapper.writeValueAsString(avro);
            kafkaTemplate.send(TOPIC, String.valueOf(avro.getUserId()), json);
            log.info("Sent UserActionAvro to topic {}: {}", TOPIC, json);
        } catch (Exception e) {
            log.error("Failed to send UserActionAvro to Kafka: {}", e.getMessage());
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