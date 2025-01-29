package com.dvFabricio.VidaLongaFlix.domain.DTOs;

import com.dvFabricio.VidaLongaFlix.domain.video.Category;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;


import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record CategoryDTO(
        UUID id,
        String name,
        Set<String> videoTitles
) {
    public CategoryDTO(UUID id, String name) {
        this(id, name, Set.of());
    }

    public CategoryDTO(Category category) {
        this(category.getId(), category.getName(),
                category.getVideos() != null
                        ? category.getVideos().stream()
                        .filter(video -> video != null && video.getTitle() != null)
                        .map(Video::getTitle)
                        .collect(Collectors.toSet())
                        : Set.of());
    }
}


