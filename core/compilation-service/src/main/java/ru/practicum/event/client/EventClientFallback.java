package ru.practicum.event.client;

import org.springframework.stereotype.Component;
import ru.practicum.event.dto.EventShortDto;

@Component
public class EventClientFallback implements EventClient {

    @Override
    public Boolean existsById(Long eventId) {
        return false;
    }

    @Override
    public EventShortDto getEventShort(Long eventId) {
        return null;
    }
}