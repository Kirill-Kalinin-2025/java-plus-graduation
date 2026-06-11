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
}