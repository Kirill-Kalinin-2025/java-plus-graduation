package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.entity.EventSimilarity;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-user-actions", containerFactory = "userActionListenerFactory")
    public void consumeUserAction(UserActionAvro action) {
        log.info("Analyzer received user action: userId={}, eventId={}", action.getUserId(), action.getEventId());

        double weight = switch (action.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };

        Optional<UserAction> existing = userActionRepository
                .findByUserId(action.getUserId()).stream()
                .filter(ua -> ua.getEventId().equals(action.getEventId()))
                .findFirst();

        if (existing.isPresent()) {
            if (weight > existing.get().getMaxWeight()) {
                existing.get().setMaxWeight(weight);
                existing.get().setTimestamp(action.getTimestamp());
                userActionRepository.save(existing.get());
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
    }

    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-similarity", containerFactory = "eventSimilarityListenerFactory")
    public void consumeSimilarity(EventSimilarityAvro similarity) {
        log.info("Analyzer received similarity: eventA={}, eventB={}, score={}", similarity.getEventA(), similarity.getEventB(), similarity.getScore());

        EventSimilarity es = EventSimilarity.builder()
                .eventA(similarity.getEventA())
                .eventB(similarity.getEventB())
                .score(similarity.getScore())
                .timestamp(similarity.getTimestamp())
                .build();
        eventSimilarityRepository.save(es);
    }
}