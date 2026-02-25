package com.dvFabricio.VidaLongaFlix.domain.category;

import java.util.UUID;

public record CategoryDTO(UUID id, String name, CategoryType type) {

    public CategoryDTO(Category category) {
        this(category.getId(), category.getName(), category.getType());
    }
}