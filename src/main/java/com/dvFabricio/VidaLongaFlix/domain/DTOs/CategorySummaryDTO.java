package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.video.Category;

import java.util.UUID;

public record CategorySummaryDTO(
        UUID id,
        String name
) {
    public CategorySummaryDTO(Category category) {
        this(category.getId(), category.getName());
    }
}
