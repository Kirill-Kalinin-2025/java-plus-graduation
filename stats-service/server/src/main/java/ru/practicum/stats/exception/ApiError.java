package ru.practicum.stats.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.stats.dto.StatsConstants;

import java.time.LocalDateTime;

public record ApiError(
        String status,
        String reason,
        String message,
        @JsonFormat(pattern = StatsConstants.DATE_TIME_PATTERN) LocalDateTime timestamp
) {
}