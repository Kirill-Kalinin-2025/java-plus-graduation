package ru.practicum.event.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;

@Component
public class EventMapper {

    public EventFullDto toFullDto(Event event, Long confirmedRequests, Double rating) {
        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(new ru.practicum.category.dto.CategoryDto() {{ setId(event.getCategoryId()); }});
        dto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);
        dto.setCreatedOn(event.getCreatedOn());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(new ru.practicum.user.dto.UserShortDto() {{ setId(event.getInitiatorId()); }});
        dto.setLocation(event.getLocation());
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState().name());
        dto.setTitle(event.getTitle());
        dto.setRating(rating != null ? rating : 0.0);
        return dto;
    }

    public EventShortDto toShortDto(Event event, Long confirmedRequests, Double rating) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(new ru.practicum.category.dto.CategoryDto() {{ setId(event.getCategoryId()); }});
        dto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(new ru.practicum.user.dto.UserShortDto() {{ setId(event.getInitiatorId()); }});
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setRating(rating != null ? rating : 0.0);
        return dto;
    }
}