package com.dvFabricio.VidaLongaFlix.domain.comment;

import java.util.UUID;

public record CommentResponseDTO(
        UUID id,
        String text,
        String date,
        UserSummary user
) {
    public record UserSummary(String id, String name) {}

    public CommentResponseDTO(Comment comment) {
        this(
                comment.getId(),
                comment.getText(),
                comment.getDate() != null ? comment.getDate().toString() : null,
                comment.getUser() != null
                        ? new UserSummary(
                                comment.getUser().getId().toString(),
                                comment.getUser().getName())
                        : null
        );
    }
}

