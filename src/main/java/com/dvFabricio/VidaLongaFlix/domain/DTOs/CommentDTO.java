package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.comment.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentDTO(
        UUID id,
        String text,
        UserDTO user,
        LocalDateTime date,
        UUID videoUuid
) {
    public CommentDTO(Comment comment) {
        this(
                comment.getId(),
                comment.getText(),
                new UserDTO(comment.getUser()),
                comment.getDate(),
                comment.getVideo().getUuid()
        );
    }
}
