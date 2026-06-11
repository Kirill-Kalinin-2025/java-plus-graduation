package ru.practicum.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {

    @GetMapping("/internal/events/{eventId}/exists")
    Boolean existsById(@PathVariable Long eventId);

    @GetMapping("/internal/events/{eventId}/published")
    Boolean isPublished(@PathVariable Long eventId);

    @GetMapping("/internal/events/{eventId}/initiator/{userId}")
    Boolean isInitiator(@PathVariable Long eventId, @PathVariable Long userId);

    @GetMapping("/internal/events/{eventId}/participantLimit")
    Integer getParticipantLimit(@PathVariable Long eventId);

    @GetMapping("/internal/events/{eventId}/requestModeration")
    Boolean isRequestModeration(@PathVariable Long eventId);
}