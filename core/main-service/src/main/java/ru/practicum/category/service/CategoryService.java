package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        if (dto.getName().length() > 50) {
            throw new BadRequestException("Имя категории не может быть длиннее 50 символов");
        }
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Категория с именем \"" + dto.getName() + "\" уже существует");
        }
        Category category = new Category();
        category.setName(dto.getName());
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto update(Long catId, CategoryDto dto) {
        if (dto.getName() != null && dto.getName().length() > 50) {
            throw new BadRequestException("Имя категории не может быть длиннее 50 символов");
        }
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
        if (!category.getName().equals(dto.getName()) && categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Категория с именем \"" + dto.getName() + "\" уже существует");
        }
        category.setName(dto.getName());
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));

        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Категория не является пустой");
        }

        categoryRepository.delete(category);
    }

    public List<CategoryDto> getCategories(int from, int size) {
        return categoryRepository.findAll(PageRequest.of(from / size, size)).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto getCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + catId + " не найдена"));
        return toDto(category);
    }

    private CategoryDto toDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }
}