package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VideoRequestDTO(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String url,
        @NotNull UUID categoryId,
        String receita,
        Double proteinas,
        Double carboidratos,
        Double gorduras,
        Double fibras
) {}