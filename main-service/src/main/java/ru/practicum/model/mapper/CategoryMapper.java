package ru.practicum.model.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.model.dto.CategoryDto;
import ru.practicum.model.dto.NewCategoryDto;
import ru.practicum.model.entity.Category;

@Component
public class CategoryMapper {

    public Category toEntity(NewCategoryDto categoryDto) {
        return Category.builder()
                .name(categoryDto.getName())
                .build();
    }

    public CategoryDto toDto(Category category) {
        if (category == null) return null;

        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}