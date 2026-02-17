package com.dvFabricio.VidaLongaFlix.services;

import com.dvFabricio.VidaLongaFlix.domain.DTOs.CategorySummaryDTO;
import com.dvFabricio.VidaLongaFlix.domain.DTOs.VideoDTO;
import com.dvFabricio.VidaLongaFlix.domain.video.Category;
import com.dvFabricio.VidaLongaFlix.domain.video.Video;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.DatabaseException;
import com.dvFabricio.VidaLongaFlix.infra.exception.database.MissingRequiredFieldException;
import com.dvFabricio.VidaLongaFlix.infra.exception.resource.ResourceNotFoundExceptions;
import com.dvFabricio.VidaLongaFlix.repositories.CategoryRepository;
import com.dvFabricio.VidaLongaFlix.repositories.VideoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;

    public VideoService(VideoRepository videoRepository, CategoryRepository categoryRepository) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void create(VideoDTO videoDTO) {
        validateVideoFields(videoDTO);

        UUID categoryId = videoDTO.category() != null ? videoDTO.category().id() : null;

        Video video = Video.builder()
                .title(videoDTO.title())
                .description(videoDTO.description())
                .url(videoDTO.url())
                .thumbnailUrl(videoDTO.thumbnailUrl()) // ✅ NEW
                .category(findCategoryById(categoryId))
                .recipe(videoDTO.recipe())
                .protein(videoDTO.protein())
                .carbohydrates(videoDTO.carbohydrates())
                .fats(videoDTO.fats())
                .fiber(videoDTO.fiber())
                .calories(videoDTO.calories())
                .build();

        saveVideo(video);
    }

    @Transactional
    public void update(UUID id, VideoDTO videoDTO) {
        validateVideoFields(videoDTO);

        Video video = findVideoById(id);

        if (!isBlank(videoDTO.title())) {
            video.setTitle(videoDTO.title());
        }
        if (!isBlank(videoDTO.description())) {
            video.setDescription(videoDTO.description());
        }
        if (!isBlank(videoDTO.url())) {
            video.setUrl(videoDTO.url());
        }

        if (!isBlank(videoDTO.thumbnailUrl())) {
            video.setThumbnailUrl(videoDTO.thumbnailUrl());
        }

        if (videoDTO.category() != null && videoDTO.category().id() != null) {
            video.setCategory(findCategoryById(videoDTO.category().id()));
        }

        if (videoDTO.recipe() != null) video.setRecipe(videoDTO.recipe());
        if (videoDTO.protein() != null) video.setProtein(videoDTO.protein());
        if (videoDTO.carbohydrates() != null) video.setCarbohydrates(videoDTO.carbohydrates());
        if (videoDTO.fats() != null) video.setFats(videoDTO.fats());
        if (videoDTO.fiber() != null) video.setFiber(videoDTO.fiber());
        if (videoDTO.calories() != null) video.setCalories(videoDTO.calories());

        saveVideo(video);
    }

    public VideoDTO findById(UUID id) {
        return new VideoDTO(findVideoById(id));
    }

    public List<VideoDTO> findAll() {
        return videoRepository.findAll().stream()
                .map(VideoDTO::new)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        Video video = findVideoById(id);
        try {
            videoRepository.delete(video);
        } catch (Exception e) {
            throw new DatabaseException("Error while deleting video with ID " + id + ": " + e.getMessage());
        }
    }

    private void saveVideo(Video video) {
        try {
            videoRepository.save(video);
        } catch (Exception e) {
            throw new DatabaseException("Error while saving video: " + e.getMessage());
        }
    }

    private Video findVideoById(UUID id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Video with ID " + id + " not found."));
    }

    private Category findCategoryById(UUID categoryId) {
        if (categoryId == null) {
            throw new MissingRequiredFieldException("category", "The video category is required.");
        }

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Category with ID " + categoryId + " not found."));
    }

    public List<VideoDTO> getMostWatchedVideos(int limit) {
        return videoRepository.findTopByOrderByViewsDesc(Pageable.ofSize(limit)).stream()
                .map(VideoDTO::new)
                .toList();
    }

    public List<VideoDTO> getLeastWatchedVideos(int limit) {
        return videoRepository.findTopByOrderByViewsAsc(Pageable.ofSize(limit)).stream()
                .map(VideoDTO::new)
                .toList();
    }

    public Map<String, Long> getTotalViewsByCategory() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Category::getName,
                        category -> videoRepository.countViewsByCategoryId(category.getId())
                ));
    }

    public double getAverageWatchTime(UUID videoId) {
        return videoRepository.findAverageWatchTimeByVideoId(videoId)
                .orElseThrow(() -> new ResourceNotFoundExceptions("Video with ID " + videoId + " has no watch time data."));
    }

    public List<VideoDTO> getVideosWithMostComments(int limit) {
        return videoRepository.findTopByOrderByCommentsCountDesc(Pageable.ofSize(limit)).stream()
                .map(VideoDTO::new)
                .toList();
    }

    private void validateVideoFields(VideoDTO videoDTO) {
        if (isBlank(videoDTO.title())) {
            throw new MissingRequiredFieldException("title", "The video title is required.");
        }
        if (isBlank(videoDTO.description())) {
            throw new MissingRequiredFieldException("description", "The video description is required.");
        }
        if (isBlank(videoDTO.url())) {
            throw new MissingRequiredFieldException("url", "The video URL is required.");
        }

        if (isBlank(videoDTO.thumbnailUrl())) {
            throw new MissingRequiredFieldException("thumbnailUrl", "The video thumbnail URL is required.");
        }

        if (videoDTO.category() == null || videoDTO.category().id() == null) {
            throw new MissingRequiredFieldException("category", "The video category is required.");
        }
    }

    private boolean isBlank(String field) {
        return field == null || field.isBlank();
    }
}
