package ru.practicum.collector.service;

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

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;

    private static final String TOPIC = "stats.user-actions.v1";

    public void sendUserAction(UserActionProto proto) {
        UserActionAvro avro = UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(mapActionType(proto.getActionType()))
                .setTimestamp(Instant.ofEpochMilli(
                        proto.getTimestamp().getSeconds() * 1000 + proto.getTimestamp().getNanos() / 1_000_000))
                .build();

        kafkaTemplate.send(TOPIC, avro);
        log.info("Sent UserActionAvro to topic {}: {}", TOPIC, avro);
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