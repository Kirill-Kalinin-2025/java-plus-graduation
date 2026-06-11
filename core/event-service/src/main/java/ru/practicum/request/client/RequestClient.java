package ru.practicum.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/event/{eventId}")
    List<ParticipationRequestDto> getEventRequests(@RequestParam Long userId, @PathVariable Long eventId);

    @PatchMapping("/internal/requests/event/{eventId}/status")
    EventRequestStatusUpdateResult updateRequestsStatus(@RequestParam Long userId,
                                                        @PathVariable Long eventId,
                                                        @RequestBody EventRequestStatusUpdateRequest request);

    @GetMapping("/internal/requests/event/{eventId}/count")
    Long countByEventIdAndStatus(@PathVariable Long eventId, @RequestParam String status);
}