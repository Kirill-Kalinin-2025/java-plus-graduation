package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-user-actions", containerFactory = "userActionListenerFactory")
    public void consumeUserAction(UserActionAvro action) {
        log.info("Analyzer received user action: userId={}, eventId={}, type={}",
                action.getUserId(), action.getEventId(), action.getActionType());

        double weight = switch (action.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };

        // Используем эффективный запрос вместо findAll + фильтрации
        Optional<UserAction> existing = userActionRepository
                .findByUserIdAndEventId(action.getUserId(), action.getEventId());

        if (existing.isPresent()) {
            UserAction userAction = existing.get();
            if (weight > userAction.getMaxWeight()) {
                userAction.setMaxWeight(weight);
                userAction.setTimestamp(action.getTimestamp());
                userActionRepository.save(userAction);
                log.info("Updated user action: userId={}, eventId={}, newWeight={}",
                        action.getUserId(), action.getEventId(), weight);
            } else {
                log.debug("Skipped update: existing weight {} >= new weight {}",
                        userAction.getMaxWeight(), weight);
            }
        } else {
            UserAction ua = UserAction.builder()
                    .userId(action.getUserId())
                    .eventId(action.getEventId())
                    .maxWeight(weight)
                    .timestamp(action.getTimestamp())
                    .build();
            userActionRepository.save(ua);
            log.info("Created new user action: userId={}, eventId={}, weight={}",
                    action.getUserId(), action.getEventId(), weight);
        }
    }

    @Transactional
    @KafkaListener(topics = "stats.events-similarity.v1", groupId = "analyzer-similarity", containerFactory = "eventSimilarityListenerFactory")
    public void consumeSimilarity(EventSimilarityAvro similarity) {
        log.info("Analyzer received similarity: eventA={}, eventB={}, score={}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore());

        // Проверяем, есть ли уже такая запись, и обновляем если нужно
        var existing = eventSimilarityRepository.findByEventAAndEventB(
                similarity.getEventA(), similarity.getEventB());

        if (existing.isPresent()) {
            var es = existing.get();
            es.setScore(similarity.getScore());
            es.setTimestamp(similarity.getTimestamp());
            eventSimilarityRepository.save(es);
            log.debug("Updated existing similarity");
        } else {
            var es = ru.practicum.analyzer.entity.EventSimilarity.builder()
                    .eventA(similarity.getEventA())
                    .eventB(similarity.getEventB())
                    .score(similarity.getScore())
                    .timestamp(similarity.getTimestamp())
                    .build();
            eventSimilarityRepository.save(es);
            log.debug("Created new similarity");
        }
    }
}