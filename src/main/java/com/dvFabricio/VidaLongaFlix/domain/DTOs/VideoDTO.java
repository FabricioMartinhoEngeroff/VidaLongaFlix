package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;

import java.util.List;
import java.util.UUID;

public record VideoDTO(
        UUID id,
        String title,
        String description,
        String url,
        UUID categoryId,
        List<CommentDTO> comments,
        int commentCount,
        int views,
        double watchTime
) {
    public VideoDTO(Video video) {
        this(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getUrl(),
                video.getCategory() != null ? video.getCategory().getId() : null,
                video.getComments() != null
                        ? video.getComments().stream().map(CommentDTO::new).toList()
                        : List.of(),
                video.getComments() != null ? video.getComments().size() : 0,
                video.getViews(),
                video.getWatchTime()
        );
    }
}
