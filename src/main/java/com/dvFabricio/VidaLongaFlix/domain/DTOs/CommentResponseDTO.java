package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.video.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponseDTO(UUID id, String text, LocalDateTime date, String userLogin, String videoTitle) {
    public CommentResponseDTO(Comment comment) {
        this(
                comment.getId(),
                comment.getText(),
                comment.getDate(),
                comment.getUser().getName(),
                comment.getVideo().getTitle()
        );
    }
}
