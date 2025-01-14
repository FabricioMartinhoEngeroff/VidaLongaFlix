package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;

import java.util.List;
import java.util.UUID;

public record VideoDTO(
        UUID id,
        String title,
        String description,
        String url,
        String categoryName,
        List<CommentDTO> comments
) {
    public VideoDTO(Video video) {
        this(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getUrl(),
                video.getCategoryName(),
                video.getCommentDTOs()
        );
    }
}