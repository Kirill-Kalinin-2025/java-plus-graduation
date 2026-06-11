package ru.practicum.category.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.category.dto.CategoryDto;

@FeignClient(name = "category-service")
public interface CategoryClient {

    @GetMapping("/internal/categories/{catId}")
    CategoryDto getCategory(@PathVariable Long catId);
}