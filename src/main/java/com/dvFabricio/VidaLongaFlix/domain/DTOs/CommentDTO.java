package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.video.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentDTO(
        UUID id,
        String text,
        LocalDateTime date,
        UUID userId,
        UUID videoId
) {
    public CommentDTO(Comment comment) {
        this(
                comment.getId(),
                comment.getText(),
                comment.getDate(),
                comment.getUser().getId(),
                comment.getVideo().getId()
        );
    }

    public CommentDTO(String text) {
        this(null, text, null, null, null);
    }
}
