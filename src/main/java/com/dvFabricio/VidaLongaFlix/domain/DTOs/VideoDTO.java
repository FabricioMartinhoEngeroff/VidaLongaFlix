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
        List<CreateCommentDTO> comments,
        int commentCount,
        int views,
        double watchTime,
        String receita,
        Double proteinas,
        Double carboidratos,
        Double gorduras,
        Double fibras
) {
    public VideoDTO(Video video) {
        this(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getUrl(),
                video.getCategory() != null ? video.getCategory().getId() : null,
                video.getComments() != null
                        ? video.getComments().stream().map(CreateCommentDTO::new).toList()
                        : List.of(),
                video.getComments() != null ? video.getComments().size() : 0,
                video.getViews(),
                video.getWatchTime(),
                video.getReceita(),
                video.getProteinas(),
                video.getCarboidratos(),
                video.getGorduras(),
                video.getFibras()
        );
    }
}

