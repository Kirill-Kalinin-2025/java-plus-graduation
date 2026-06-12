package ru.practicum.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final RequestService requestService;
    private final RequestRepository requestRepository;

    @GetMapping("/event/{eventId}")
    public List<ParticipationRequestDto> getEventRequests(@RequestParam("userId") Long userId,
                                                          @PathVariable Long eventId) {
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/event/{eventId}/status")
    public EventRequestStatusUpdateResult updateRequestsStatus(@PathVariable Long eventId,
                                                               @RequestBody EventRequestStatusUpdateRequest request) {
        return requestService.updateRequestsStatus(request.getUserId(), eventId, request);
    }

    @GetMapping("/event/{eventId}/count")
    public Long countByEventIdAndStatus(@PathVariable Long eventId, @RequestParam("status") String status) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.valueOf(status));
    }
}