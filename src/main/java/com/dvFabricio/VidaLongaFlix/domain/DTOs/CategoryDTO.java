package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.category.Category;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;


import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


public record CategoryDTO(
        UUID uuid,
        String name,
        Set<String> videoTitles
) {
    public CategoryDTO(Category category) {
        this(category.getUuid(), category.getName(),
                category.getVideos().stream()
                        .filter(video -> video != null && video.getTitle() != null)
                        .map(video -> video.getTitle())
                        .collect(Collectors.toSet()));
    }
}
