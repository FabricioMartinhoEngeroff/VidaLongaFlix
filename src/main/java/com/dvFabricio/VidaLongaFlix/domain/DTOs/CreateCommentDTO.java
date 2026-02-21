package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCommentDTO(
        @NotBlank(message = "Comment text is required.")
        String text,
        @NotNull UUID videoId
) {}
