package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;
import java.util.UUID;

public record CommentResponseDTO(
        UUID id,
        String text,
        UUID userId,
        UUID videoId
) {
    public CommentResponseDTO(Comment comment) {
        this(
                comment.getId(),
                comment.getText(),
                comment.getUser() != null ? comment.getUser().getId() : null,
                comment.getVideo() != null ? comment.getVideo().getId() : null
        );
    }
}

