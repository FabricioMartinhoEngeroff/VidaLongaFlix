package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VideoRequestDTO(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String url,
        @NotBlank String cover,
        @NotNull UUID categoryId,
        String recipe,
        Double protein,
        Double carbs,
        Double fat,
        Double fiber,
        Double calories
) {}
