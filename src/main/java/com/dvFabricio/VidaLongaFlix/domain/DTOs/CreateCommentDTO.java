package com.dvFabricio.VidaLongaFlix.domain.DTOs;


import com.dvFabricio.VidaLongaFlix.domain.video.Comment;

import java.util.UUID;

public record CreateCommentDTO(String text, UUID userId, UUID videoId) {

    public CreateCommentDTO(Comment comment) {
        this(
                comment.getText(),
                comment.getUser() != null ? comment.getUser().getId() : null,
                comment.getVideo() != null ? comment.getVideo().getId() : null
        );
    }

}
