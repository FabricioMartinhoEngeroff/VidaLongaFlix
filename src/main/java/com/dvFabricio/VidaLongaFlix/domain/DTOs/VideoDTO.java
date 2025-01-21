package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;

import java.util.List;
import java.util.UUID;

public record VideoDTO(
        Long id,
        UUID uuid,
        String title,
        String description,
        String url,
        UUID categoryUuid,
        List<CommentDTO> comments,
        int commentCount
) {
    public VideoDTO(Video video) {
        this(
                video.getId(),
                video.getUuid(),
                video.getTitle(),
                video.getDescription(),
                video.getUrl(),
                video.getCategory() != null ? video.getCategory().getUuid() : null,
                video.getCommentDTOs(),
                video.getCommentCount()
        );
    }
}
