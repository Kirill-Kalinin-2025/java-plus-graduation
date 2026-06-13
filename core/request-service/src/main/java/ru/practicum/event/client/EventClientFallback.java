package ru.practicum.event.client;

import org.springframework.stereotype.Component;

@Component
public class EventClientFallback implements EventClient {

    @Override
    public Boolean existsById(Long eventId) {
        return false;
    }

    @Override
    public Boolean isPublished(Long eventId) {
        return false;
    }

    @Override
    public Boolean isInitiator(Long eventId, Long userId) {
        return false;
    }

    @Override
    public Integer getParticipantLimit(Long eventId) {
        return null;
    }

    @Override
    public Boolean isRequestModeration(Long eventId) {
        return null;
    }
}