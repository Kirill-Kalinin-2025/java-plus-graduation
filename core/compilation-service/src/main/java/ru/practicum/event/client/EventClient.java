package ru.practicum.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.event.dto.EventShortDto;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {

    @GetMapping("/internal/events/{eventId}/exists")
    Boolean existsById(@PathVariable Long eventId);

    @GetMapping("/internal/events/{eventId}/short")
    EventShortDto getEventShort(@PathVariable Long eventId);
}