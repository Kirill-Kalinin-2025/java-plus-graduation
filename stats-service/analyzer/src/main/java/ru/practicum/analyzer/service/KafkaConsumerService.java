package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayInputStream;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Transactional
    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-user-actions",
            containerFactory = "userActionListenerFactory")
    public void consumeUserAction(byte[] data) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
            SpecificDatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.class);
            UserActionAvro action = reader.read(null, decoder);

            log.info("Analyzer received user action: userId={}, eventId={}, type={}",
                    action.getUserId(), action.getEventId(), action.getActionType());

            double weight = switch (action.getActionType()) {
                case VIEW -> 0.4;
                case REGISTER -> 0.8;
                case LIKE -> 1.0;
            };

            Optional<UserAction> existing = userActionRepository
                    .findByUserIdAndEventId(action.getUserId(), action.getEventId());

            if (existing.isPresent()) {
                UserAction userAction = existing.get();
                if (weight > userAction.getMaxWeight()) {
                    userAction.setMaxWeight(weight);
                    userAction.setTimestamp(action.getTimestamp());
                    userActionRepository.save(userAction);
                }
            } else {
                UserAction ua = UserAction.builder()
                        .userId(action.getUserId())
                        .eventId(action.getEventId())
                        .maxWeight(weight)
                        .timestamp(action.getTimestamp())
                        .build();
                userActionRepository.save(ua);
            }
        } catch (Exception e) {
            log.error("Failed to deserialize UserActionAvro: {}", e.getMessage(), e);
        }
    }

    @Transactional
    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-similarity",
            containerFactory = "eventSimilarityListenerFactory")
    public void consumeSimilarity(byte[] data) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
            SpecificDatumReader<EventSimilarityAvro> reader = new SpecificDatumReader<>(EventSimilarityAvro.class);
            EventSimilarityAvro similarity = reader.read(null, decoder);

            log.info("Analyzer received similarity: eventA={}, eventB={}, score={}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore());

            var existing = eventSimilarityRepository.findByEventAAndEventB(
                    similarity.getEventA(), similarity.getEventB());

            if (existing.isPresent()) {
                var es = existing.get();
                es.setScore(similarity.getScore());
                es.setTimestamp(similarity.getTimestamp());
                eventSimilarityRepository.save(es);
            } else {
                var es = ru.practicum.analyzer.entity.EventSimilarity.builder()
                        .eventA(similarity.getEventA())
                        .eventB(similarity.getEventB())
                        .score(similarity.getScore())
                        .timestamp(similarity.getTimestamp())
                        .build();
                eventSimilarityRepository.save(es);
            }
        } catch (Exception e) {
            log.error("Failed to deserialize EventSimilarityAvro: {}", e.getMessage(), e);
        }
    }
}