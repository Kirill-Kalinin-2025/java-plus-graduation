package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.enums.EventState;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @GetMapping("/{eventId}/exists")
    public Boolean existsById(@PathVariable Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @GetMapping("/category/{categoryId}/exists")
    public Boolean existsByCategoryId(@PathVariable Long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }

    @GetMapping("/{eventId}/published")
    public Boolean isPublished(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> event.getState() == EventState.PUBLISHED)
                .orElse(false);
    }

    @GetMapping("/{eventId}/initiator/{userId}")
    public Boolean isInitiator(@PathVariable Long eventId, @PathVariable Long userId) {
        return eventRepository.findById(eventId)
                .map(event -> event.getInitiator().getId().equals(userId))
                .orElse(false);
    }

    @GetMapping("/{eventId}/participantLimit")
    public Integer getParticipantLimit(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> event.getParticipantLimit())
                .orElse(null);
    }

    @GetMapping("/{eventId}/requestModeration")
    public Boolean isRequestModeration(@PathVariable Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> event.getRequestModeration())
                .orElse(null);
    }

    @GetMapping("/{eventId}/short")
    public EventShortDto getEventShort(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        return eventMapper.toShortDto(event, 0L, event.getViews());
    }
}