package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(Boolean.TRUE.equals(dto.getPinned()));
        compilation.setEvents(resolveEvents(dto.getEvents()));

        Compilation saved = compilationRepository.save(compilation);
        Compilation detailed = compilationRepository.findDetailedById(saved.getId())
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + saved.getId() + " was not found"));
        return compilationMapper.toDto(detailed);
    }

    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (dto.getEvents() != null) {
            compilation.setEvents(resolveEvents(dto.getEvents()));
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }

        compilationRepository.save(compilation);
        Compilation detailed = compilationRepository.findDetailedById(compilation.getId())
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compilation.getId() + " was not found"));
        return compilationMapper.toDto(detailed);
    }

    @Transactional
    public void delete(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        compilationRepository.deleteById(compId);
    }

    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        List<Long> compilationIds = compilationRepository.findIdsByPinned(pinned, PageRequest.of(from / size, size))
                .getContent();
        if (compilationIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> orderById = new LinkedHashMap<>();
        for (int index = 0; index < compilationIds.size(); index++) {
            orderById.put(compilationIds.get(index), index);
        }

        return compilationRepository.findAllDetailedByIdIn(compilationIds).stream()
                .sorted(Comparator.comparingInt(c -> orderById.getOrDefault(c.getId(), Integer.MAX_VALUE)))
                .map(compilationMapper::toDto)
                .collect(Collectors.toList());
    }

    public CompilationDto getCompilation(Long compId) {
        Compilation detailed = compilationRepository.findDetailedById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        return compilationMapper.toDto(detailed);
    }

    private Set<Event> resolveEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.size() != eventIds.size()) {
            Set<Long> foundIds = events.stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());
            Long missingEventId = eventIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new NotFoundException("Event with id=" + missingEventId + " was not found");
        }

        return events.stream()
                .sorted(Comparator.comparing(Event::getId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}