package ru.practicum.compilation.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateCompilationRequest {

    private Set<@Positive(message = "Идентификатор события должен быть положительным") Long> events;

    private Boolean pinned;

    @Pattern(regexp = ".*\\S.*", message = "Заголовок подборки не может быть пустым")
    @Size(min = 1, max = 50, message = "Заголовок подборки должен содержать от 1 до 50 символов")
    private String title;
}