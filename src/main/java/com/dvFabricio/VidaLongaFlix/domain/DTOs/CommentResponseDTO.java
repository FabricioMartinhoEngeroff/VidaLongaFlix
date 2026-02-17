package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.video.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponseDTO(
        UUID id,
        String text,
        LocalDateTime date,
        UserSummaryDTO user
) {
    public CommentResponseDTO(Comment comment) {
        this(
                comment.getId(),
                comment.getText(),
                comment.getDate(),
                new UserSummaryDTO(comment.getUser())
        );
    }
}

