package com.dvFabricio.VidaLongaFlix.domain.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MenuRequestDTO(

        @NotBlank(message = "Título não pode estar vazio")
        String title,

        @NotBlank(message = "Descrição não pode estar vazia")
        String description,

        String cover,

        @NotNull(message = "Categoria é obrigatória")
        UUID categoryId,

        String recipe,
        String nutritionistTips,
        Double protein,
        Double carbs,
        Double fat,
        Double fiber,
        Double calories
) {}