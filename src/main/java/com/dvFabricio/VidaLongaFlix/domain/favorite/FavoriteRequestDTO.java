package com.dvFabricio.VidaLongaFlix.domain.favorite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavoriteRequestDTO(
        @NotBlank String itemId,
        @NotNull FavoriteContentType itemType
) {}
