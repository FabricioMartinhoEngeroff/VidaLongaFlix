package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(
        @NotBlank(message = "Category name is required.")
        @Size(max = 80, message = "Category name must be at most 80 characters.")
        String name
) {}