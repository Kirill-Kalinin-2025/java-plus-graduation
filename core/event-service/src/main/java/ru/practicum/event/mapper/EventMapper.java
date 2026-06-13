package ru.practicum.event.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.category.client.CategoryClient;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;
import ru.practicum.user.client.UserClient;
import ru.practicum.user.dto.UserShortDto;

@Component
public class EventMapper {

    public EventFullDto toFullDto(Event event, Long confirmedRequests, Long views) {
        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(new CategoryDto() {{ setId(event.getCategoryId()); }});
        dto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);
        dto.setCreatedOn(event.getCreatedOn());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(new UserShortDto() {{ setId(event.getInitiatorId()); }});
        dto.setLocation(event.getLocation());
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState().name());
        dto.setTitle(event.getTitle());
        dto.setViews(views != null ? views : 0);
        return dto;
    }

    public EventShortDto toShortDto(Event event, Long confirmedRequests, Long views) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(new CategoryDto() {{ setId(event.getCategoryId()); }});
        dto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0);
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(new UserShortDto() {{ setId(event.getInitiatorId()); }});
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setViews(views != null ? views : 0);
        return dto;
    }
}