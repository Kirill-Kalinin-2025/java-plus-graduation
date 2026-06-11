package ru.practicum.request.client;

import org.springframework.stereotype.Component;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.Collections;
import java.util.List;

@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        return Collections.emptyList();
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId,
                                                               EventRequestStatusUpdateRequest request) {
        return new EventRequestStatusUpdateResult(Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public Long countByEventIdAndStatus(Long eventId, String status) {
        return 0L;
    }
}