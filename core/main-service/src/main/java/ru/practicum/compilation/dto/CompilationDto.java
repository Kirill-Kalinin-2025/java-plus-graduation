package ru.practicum.compilation.dto;

import lombok.Getter;
import lombok.Setter;
import ru.practicum.event.dto.EventShortDto;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CompilationDto {
    private List<EventShortDto> events = new ArrayList<>();
    private Long id;
    private Boolean pinned;
    private String title;
}