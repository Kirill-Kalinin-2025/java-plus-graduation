package ru.practicum.event.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/{eventId}/exists")
    Boolean existsById(@PathVariable Long eventId);

    @GetMapping("/internal/events/{eventId}/published")
    Boolean isPublished(@PathVariable Long eventId);
}