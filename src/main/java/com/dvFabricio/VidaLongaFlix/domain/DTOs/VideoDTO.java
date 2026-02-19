package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.video.Video;

import java.util.List;
import java.util.UUID;

public record VideoDTO(
        UUID id,
        String title,
        String description,
        String url,
        String cover,
        CategorySummaryDTO category,
        List<CommentResponseDTO> comments,
        int commentCount,
        int views,
        double watchTime,
        String recipe,
        Double protein,
        Double carbs,
        Double fat,
        Double fiber,
        Double calories,
        int likesCount,
        boolean favorited
) {
    public VideoDTO(Video video) {
        this(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getUrl(),
                video.getCover(),
                video.getCategory() != null ? new CategorySummaryDTO(video.getCategory()) : null,
                video.getComments() != null
                        ? video.getComments().stream().map(CommentResponseDTO::new).toList()
                        : List.of(),
                video.getComments() != null ? video.getComments().size() : 0,
                video.getViews(),
                video.getWatchTime(),
                video.getRecipe(),
                video.getProtein(),
                video.getCarbs(),
                video.getFat(),
                video.getFiber(),
                video.getCalories(),
                video.getLikesCount(),
                video.isFavorited()
        );
    }
}