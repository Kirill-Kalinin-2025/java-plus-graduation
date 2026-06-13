package ru.practicum.category.client;

import org.springframework.stereotype.Component;
import ru.practicum.category.dto.CategoryDto;

@Component
public class CategoryClientFallback implements CategoryClient {

    @Override
    public CategoryDto getCategory(Long catId) {
        return null;
    }
}