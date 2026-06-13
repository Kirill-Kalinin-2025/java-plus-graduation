package ru.practicum.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.client.EventClient;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.client.UserClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Transactional
    @Override
    public ParticipationRequestDto create(Long userId, Long eventId) {
        if (!userClient.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        if (!eventClient.existsById(eventId)) {
            throw new NotFoundException("Ивент с id =" + eventId + " не найден");
        }
        if (eventClient.isInitiator(eventId, userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии");
        }
        if (!eventClient.isPublished(eventId)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Повторный запрос на участие невозможен");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Integer participantLimit = eventClient.getParticipantLimit(eventId);

        if (participantLimit != null && participantLimit != 0 && confirmedCount >= participantLimit) {
            throw new ConflictException("Достигнут лимит участников");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);
        request.setEventId(eventId);
        request.setCreated(LocalDateTime.now());

        if (participantLimit == null || participantLimit == 0 || !eventClient.isRequestModeration(eventId)) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(RequestStatus.PENDING);
        }

        return toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getByUser(Long userId) {
        return requestRepository.findByRequesterId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));
        request.setStatus(RequestStatus.CANCELED);
        return toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        if (!eventClient.isInitiator(eventId, userId)) {
            throw new ConflictException("Только инициатор события может просматривать заявки");
        }
        return requestRepository.findByEventId(eventId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public EventRequestStatusUpdateResult updateRequestsStatus(Long userId, Long eventId,
                                                               EventRequestStatusUpdateRequest updateRequest) {
        if (!eventClient.isInitiator(eventId, userId)) {
            throw new ConflictException("Только инициатор события может изменять статусы заявок");
        }

        RequestStatus newStatus = RequestStatus.valueOf(updateRequest.getStatus().toUpperCase());
        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());

        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("Один или несколько запросов не найдены");
        }

        for (ParticipationRequest r : requests) {
            if (r.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок в состоянии PENDING");
            }
            if (!r.getEventId().equals(eventId)) {
                throw new ConflictException("Заявка относится к другому событию");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (newStatus == RequestStatus.CONFIRMED) {
            Integer limit = eventClient.getParticipantLimit(eventId);
            int available = (limit == null || limit == 0) ? Integer.MAX_VALUE : limit - (int) requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (available <= 0) {
                throw new ConflictException("Достигнут лимит участников");
            }

            int toConfirm = Math.min(available, requests.size());
            for (int i = 0; i < requests.size(); i++) {
                ParticipationRequest req = requests.get(i);
                if (i < toConfirm) {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(toDto(req));
                } else {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(toDto(req));
                }
            }
        } else {
            for (ParticipationRequest req : requests) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(toDto(req));
            }
        }

        requestRepository.saveAll(requests);
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);
        return result;
    }

    private ParticipationRequestDto toDto(ParticipationRequest request) {
        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(request.getId());
        dto.setCreated(request.getCreated());
        dto.setEvent(request.getEventId());
        dto.setRequester(request.getRequesterId());
        dto.setStatus(request.getStatus().name());
        return dto;
    }
}